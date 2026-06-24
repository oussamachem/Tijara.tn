package com.smartboutique.service;

import com.smartboutique.dto.PageResponse;
import com.smartboutique.dto.ReturnRequest;
import com.smartboutique.dto.ReturnResponse;
import com.smartboutique.entity.Product;
import com.smartboutique.entity.Return;
import com.smartboutique.entity.Sale;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.mapper.ReturnMapper;
import com.smartboutique.repository.ProductRepository;
import com.smartboutique.repository.ReturnRepository;
import com.smartboutique.repository.SaleItemRepository;
import com.smartboutique.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Gestion des retours produits.
 *
 * <p>Un retour est rattache a une vente d'origine et a un produit. La quantite est reintegree
 * au stock dans la meme transaction. On empeche de retourner plus que la quantite reellement
 * vendue (moins ce qui a deja ete retourne) sur cette vente.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnService {

    private final ReturnRepository returnRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final ReturnMapper returnMapper;

    @Transactional
    public ReturnResponse createReturn(ReturnRequest request) {
        Sale sale = saleRepository.findById(request.saleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vente", request.saleId()));
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit", request.productId()));

        int sold = saleItemRepository.sumQuantityBySaleAndProduct(sale.getId(), product.getId());
        if (sold == 0) {
            throw new BusinessException(
                    "Ce produit ne fait pas partie de la vente #" + sale.getId(),
                    HttpStatus.CONFLICT);
        }

        int alreadyReturned = returnRepository.sumReturnedBySaleAndProduct(sale.getId(), product.getId());
        int returnable = sold - alreadyReturned;
        if (request.quantity() > returnable) {
            throw new BusinessException(
                    "Retour impossible : quantite demandee " + request.quantity()
                            + " superieure au retournable " + returnable
                            + " (vendu " + sold + ", deja retourne " + alreadyReturned + ")",
                    HttpStatus.CONFLICT);
        }

        Return ret = Return.builder()
                .sale(sale)
                .product(product)
                .quantity(request.quantity())
                .reason(request.reason())
                .build();
        ret = returnRepository.save(ret);

        // Reintegration du stock.
        productRepository.incrementStock(product.getId(), request.quantity());

        log.info("Retour id={} enregistre : produit {} x{} sur vente #{} (motif: {})",
                ret.getId(), product.getReference(), request.quantity(), sale.getId(), request.reason());
        return returnMapper.toResponse(ret);
    }

    /** Historique pagine des retours, filtre optionnel par periode (dates incluses). */
    @Transactional(readOnly = true)
    public PageResponse<ReturnResponse> searchReturns(LocalDate from, LocalDate to, Pageable pageable) {
        LocalDateTime start = from != null ? from.atStartOfDay() : null;
        LocalDateTime end = to != null ? to.plusDays(1).atStartOfDay() : null;
        Page<ReturnResponse> page = returnRepository.searchReturns(start, end, pageable);
        return PageResponse.of(page, page.getContent());
    }
}
