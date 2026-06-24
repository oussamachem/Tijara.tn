package com.smartboutique.service;

import com.smartboutique.dto.PageResponse;
import com.smartboutique.dto.SaleItemRequest;
import com.smartboutique.dto.SaleRequest;
import com.smartboutique.dto.SaleResponse;
import com.smartboutique.dto.SaleSummaryResponse;
import com.smartboutique.entity.Product;
import com.smartboutique.entity.Sale;
import com.smartboutique.entity.SaleItem;
import com.smartboutique.entity.User;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.mapper.SaleMapper;
import com.smartboutique.repository.ProductRepository;
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
 * Gestion des ventes.
 *
 * <p><b>Transaction (tout ou rien)</b> : creation de la vente, des lignes et decrement du stock
 * dans une seule transaction. Toute erreur (stock insuffisant, produit introuvable, remise invalide)
 * provoque un rollback complet.</p>
 *
 * <p><b>Concurrence</b> : le stock est decremente via une mise a jour atomique conditionnelle
 * {@code UPDATE products SET quantity = quantity - :q WHERE id = :id AND quantity >= :q}. Si 0 ligne
 * est affectee, le stock est insuffisant (eventuellement a cause d'une vente concurrente) et on leve
 * une 409. Ce choix evite tout survente sans maintenir de verrou applicatif long ; l'atomicite est
 * garantie par la BDD. Les lignes sont traitees triees par id produit pour limiter les interblocages.</p>
 *
 * <p><b>Prix</b> : le prix unitaire est copie depuis le produit au moment de la vente, afin qu'une
 * modification de prix ulterieure ne reecrive pas l'historique.</p>
 *
 * <p><b>Remise</b> : montant fixe (meme devise) soustrait du sous-total. Un total negatif est refuse.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
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

        // Tri par id produit pour reduire le risque d'interblocage entre ventes concurrentes.
        List<SaleItemRequest> items = request.items().stream()
                .sorted(Comparator.comparing(SaleItemRequest::productId))
                .toList();

        for (SaleItemRequest line : items) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", line.productId()));

            // Decrement atomique conditionnel : anti-survente garanti par la BDD.
            int updated = productRepository.decrementStockIfAvailable(product.getId(), line.quantity());
            if (updated == 0) {
                throw new BusinessException(
                        "Stock insuffisant pour le produit '" + product.getName()
                                + "' (reference " + product.getReference() + ") : demande "
                                + line.quantity() + ", disponible " + product.getQuantity(),
                        HttpStatus.CONFLICT);
            }

            // Capture du prix au moment de la vente.
            BigDecimal unitPrice = product.getSalePrice();
            BigDecimal linePrice = unitPrice.multiply(BigDecimal.valueOf(line.quantity()));

            SaleItem saleItem = SaleItem.builder()
                    .product(product)
                    .quantity(line.quantity())
                    .unitPrice(unitPrice)
                    .totalPrice(linePrice)
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
        log.info("Vente id={} enregistree par {} : {} article(s), total {}",
                saved.getId(), seller.getEmail(), saved.getItems().size(), total);
        return saleMapper.toResponse(saved);
    }

    /** Detail d'une vente (lignes + produits + vendeur), charge sans N+1 via @EntityGraph. */
    @Transactional(readOnly = true)
    public SaleResponse findById(Long id) {
        Sale sale = saleRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vente", id));
        return saleMapper.toResponse(sale);
    }

    /**
     * Historique pagine des ventes, filtres optionnels par periode (dates incluses) et vendeur.
     * Renvoie une projection legere (sans les lignes).
     */
    @Transactional(readOnly = true)
    public PageResponse<SaleSummaryResponse> searchHistory(LocalDate from, LocalDate to,
                                                           Long sellerId, Pageable pageable) {
        LocalDateTime start = from != null ? from.atStartOfDay() : null;
        // 'to' inclus : on borne a la fin de la journee (jour suivant a 00:00, exclu).
        LocalDateTime end = to != null ? to.plusDays(1).atStartOfDay() : null;

        Page<SaleSummaryResponse> page = saleRepository.searchHistory(start, end, sellerId, pageable);
        return PageResponse.of(page, page.getContent());
    }
}
