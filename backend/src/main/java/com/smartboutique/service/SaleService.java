package com.smartboutique.service;

import com.smartboutique.dto.PageResponse;
import com.smartboutique.dto.SaleItemRequest;
import com.smartboutique.dto.SaleRequest;
import com.smartboutique.dto.SaleResponse;
import com.smartboutique.dto.SaleSummaryResponse;
import com.smartboutique.entity.ProductVariant;
import com.smartboutique.entity.Sale;
import com.smartboutique.entity.SaleItem;
import com.smartboutique.entity.User;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.mapper.SaleMapper;
import com.smartboutique.repository.ProductVariantRepository;
import com.smartboutique.repository.SaleRepository;
import com.smartboutique.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Gestion des ventes (au grain VARIANTE depuis la Phase 9).
 *
 * <p><b>Transaction (tout ou rien)</b> : vente + lignes + decrement du stock VARIANTE dans une
 * seule transaction ; toute erreur provoque un rollback complet.</p>
 *
 * <p><b>Concurrence</b> : decrement atomique conditionnel sur {@code product_variant.quantity}
 * ({@code UPDATE ... WHERE quantity >= :q}). 0 ligne affectee => stock insuffisant (419/409),
 * pas de survente. Lignes triees par id variante pour limiter les interblocages.</p>
 *
 * <p><b>Prix</b> : prix unitaire = prix du PRODUIT, capture a la vente. Couleur/taille/reference
 * variante sont denormalises sur la ligne pour figer l'historique.</p>
 *
 * <p><b>Remise</b> : montant fixe soustrait du sous-total ; total negatif refuse.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final SaleMapper saleMapper;

    @Transactional
    public SaleResponse createSale(SaleRequest request, Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendeur", sellerId));

        Sale sale = Sale.builder()
                .seller(seller)
                .paymentMethod(request.paymentMethod())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

        // Tri par id variante pour reduire le risque d'interblocage entre ventes concurrentes.
        List<SaleItemRequest> items = request.items().stream()
                .sorted(Comparator.comparing(SaleItemRequest::variantId))
                .toList();

        for (SaleItemRequest line : items) {
            ProductVariant variant = variantRepository.findById(line.variantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variante", line.variantId()));

            // Decrement atomique conditionnel de la VARIANTE : anti-survente garanti par la BDD.
            int updated = variantRepository.decrementStockIfAvailable(variant.getId(), line.quantity());
            if (updated == 0) {
                throw new BusinessException(
                        "Stock insuffisant pour la declinaison '" + variant.getReference()
                                + "' (" + variant.getColor().getName() + " / taille " + variant.getSize()
                                + ") : demande " + line.quantity() + ", disponible " + variant.getQuantity(),
                        HttpStatus.CONFLICT);
            }

            // Capture du prix (prix produit) + denormalisation des attributs variante.
            BigDecimal unitPrice = variant.getProduct().getSalePrice();
            BigDecimal linePrice = unitPrice.multiply(BigDecimal.valueOf(line.quantity()));

            SaleItem saleItem = SaleItem.builder()
                    .variant(variant)
                    .quantity(line.quantity())
                    .unitPrice(unitPrice)
                    .totalPrice(linePrice)
                    .variantReference(variant.getReference())
                    .productName(variant.getProduct().getName())
                    .colorName(variant.getColor().getName())
                    .size(variant.getSize())
                    .build();
            sale.addItem(saleItem);

            subtotal = subtotal.add(linePrice);
        }

        BigDecimal discount = request.discount() != null ? request.discount() : BigDecimal.ZERO;
        BigDecimal total = subtotal.subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    "La remise (" + discount + ") est superieure au sous-total (" + subtotal + ")",
                    HttpStatus.BAD_REQUEST);
        }

        sale.setDiscount(discount);
        sale.setTotalAmount(total);

        Sale saved = saleRepository.save(sale);
        log.info("Vente id={} enregistree par {} : {} ligne(s), total {}",
                saved.getId(), seller.getEmail(), saved.getItems().size(), total);
        return saleMapper.toResponse(saved);
    }

    /** Detail d'une vente (lignes denormalisees + vendeur), charge sans N+1 via @EntityGraph. */
    @Transactional(readOnly = true)
    public SaleResponse findById(Long id) {
        Sale sale = saleRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vente", id));
        return saleMapper.toResponse(sale);
    }

    /** Historique pagine des ventes, filtres optionnels par periode (dates incluses) et vendeur. */
    @Transactional(readOnly = true)
    public PageResponse<SaleSummaryResponse> searchHistory(LocalDate from, LocalDate to,
                                                           Long sellerId, Pageable pageable) {
        LocalDateTime start = from != null ? from.atStartOfDay() : null;
        LocalDateTime end = to != null ? to.plusDays(1).atStartOfDay() : null;
        Page<SaleSummaryResponse> page = saleRepository.searchHistory(start, end, sellerId, pageable);
        return PageResponse.of(page, page.getContent());
    }
}
