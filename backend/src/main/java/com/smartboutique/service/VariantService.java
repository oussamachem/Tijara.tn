package com.smartboutique.service;

import com.smartboutique.dto.AddVariantRequest;
import com.smartboutique.dto.VariantResponse;
import com.smartboutique.dto.VariantScanResponse;
import com.smartboutique.entity.Color;
import com.smartboutique.entity.Product;
import com.smartboutique.entity.ProductVariant;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.DuplicateResourceException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.mapper.ProductMapper;
import com.smartboutique.repository.ColorRepository;
import com.smartboutique.repository.ProductRepository;
import com.smartboutique.repository.ProductVariantRepository;
import com.smartboutique.repository.SaleItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Operations au grain VARIANTE : ajout/retrait, stock, resolution scan (QR), QR, alertes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ColorRepository colorRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductMapper productMapper;
    private final VariantSupport variantSupport;
    private final QrCodeService qrCodeService;

    // ---------------------------------- Scan / lecture ----------------------------------

    /** Resout une variante par le contenu de son QR Code (sa reference) — vente par scan. */
    @Transactional(readOnly = true)
    public VariantScanResponse findByQrContent(String code) {
        ProductVariant v = variantRepository.findByQrCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune variante ne correspond a ce QR Code (" + code + ")"));
        return toScan(v);
    }

    /** Variantes en rupture ou sous leur seuil d'alerte. */
    @Transactional(readOnly = true)
    public List<VariantScanResponse> findLowStock() {
        return variantRepository.findLowStockVariants().stream().map(this::toScan).toList();
    }

    /** Image PNG du QR Code d'une variante (impression etiquette). */
    @Transactional(readOnly = true)
    public byte[] generateQrPng(Long variantId) {
        return qrCodeService.generatePng(getVariant(variantId).getQrCode());
    }

    // -------------------------------- Ajout / suppression --------------------------------

    @Transactional
    public VariantResponse addVariant(Long productId, AddVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", productId));
        Color color = colorRepository.findById(request.colorId())
                .orElseThrow(() -> new ResourceNotFoundException("Couleur", request.colorId()));
        variantSupport.validateSize(product.getCategory(), request.size());

        if (variantRepository.existsByProductIdAndColorIdAndSize(productId, request.colorId(), request.size())) {
            throw new DuplicateResourceException("Cette declinaison (couleur + taille) existe deja pour ce produit");
        }
        String ref = variantSupport.buildVariantReference(product.getReference(), request.size(), color.getName());
        if (variantRepository.existsByReference(ref)) {
            throw new DuplicateResourceException("Reference de variante en conflit : " + ref);
        }
        ProductVariant v = ProductVariant.builder()
                .product(product)
                .color(color)
                .size(request.size())
                .quantity(request.quantity())
                .seuilAlerte(request.seuilAlerte() != null ? request.seuilAlerte() : 0)
                .reference(ref)
                .qrCode(ref)
                .build();
        v = variantRepository.save(v);
        log.info("Variante ajoutee : {} (produit id={})", ref, productId);
        return productMapper.toVariantResponse(v);
    }

    /** Retire une variante. Interdit si c'est la derniere (invariant) ou si elle a deja ete vendue. */
    @Transactional
    public void removeVariant(Long variantId) {
        ProductVariant v = getVariant(variantId);
        if (saleItemRepository.existsByVariant_Id(variantId)) {
            throw new BusinessException(
                    "Impossible de supprimer une variante deja vendue (historique)", HttpStatus.CONFLICT);
        }
        if (variantRepository.countByProductId(v.getProduct().getId()) <= 1) {
            throw new BusinessException(
                    "Impossible de supprimer la derniere variante d'un produit", HttpStatus.CONFLICT);
        }
        variantRepository.delete(v);
        log.warn("[AUDIT] Variante supprimee : {} (id={})", v.getReference(), variantId);
    }

    // ------------------------------------- Stock -------------------------------------

    @Transactional
    public VariantResponse setStock(Long variantId, int quantity) {
        ProductVariant v = getVariant(variantId);
        int before = v.getQuantity();
        v.setQuantity(quantity);
        log.info("[AUDIT] Stock variante id={} fixe : {} -> {}", variantId, before, quantity);
        return productMapper.toVariantResponse(variantRepository.save(v));
    }

    @Transactional
    public VariantResponse adjustStock(Long variantId, int delta) {
        ProductVariant v = getVariant(variantId);
        int result = v.getQuantity() + delta;
        if (result < 0) {
            throw new BusinessException(
                    "Ajustement impossible : stock negatif (actuel " + v.getQuantity() + ", variation " + delta + ")",
                    HttpStatus.BAD_REQUEST);
        }
        v.setQuantity(result);
        return productMapper.toVariantResponse(variantRepository.save(v));
    }

    // ------------------------------------ Helpers ------------------------------------

    private ProductVariant getVariant(Long id) {
        return variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Variante", id));
    }

    private VariantScanResponse toScan(ProductVariant v) {
        Product p = v.getProduct();
        Color c = v.getColor();
        return new VariantScanResponse(
                v.getId(), v.getReference(),
                p.getId(), p.getName(), p.getSalePrice(),
                c != null ? c.getId() : null,
                c != null ? c.getName() : null,
                c != null ? c.getHex() : null,
                v.getSize(), v.getQuantity(), v.getSeuilAlerte(),
                productMapper.isLowStock(v));
    }
}
