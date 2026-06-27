package com.smartboutique.service;

import com.smartboutique.dto.*;
import com.smartboutique.entity.Category;
import com.smartboutique.entity.Color;
import com.smartboutique.entity.Product;
import com.smartboutique.entity.ProductImage;
import com.smartboutique.entity.ProductVariant;
import com.smartboutique.entity.Size;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.DuplicateResourceException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.mapper.ProductMapper;
import com.smartboutique.repository.CategoryRepository;
import com.smartboutique.repository.ColorRepository;
import com.smartboutique.repository.ProductRepository;
import com.smartboutique.repository.SaleItemRepository;
import com.smartboutique.repository.SizeRepository;
import com.smartboutique.repository.specification.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Gestion des produits (modeles) et de leurs variantes a la creation. Le stock vit au grain
 * variante (cf. {@link VariantService}). Prix au niveau produit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductMapper productMapper;
    private final VariantSupport variantSupport;
    private final FileStorageService fileStorageService;

    // -------------------------------- Lecture / recherche --------------------------------

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String name, String reference, Long categoryId, Pageable pageable) {
        Specification<Product> spec = Specification
                .where(ProductSpecifications.nameContains(name))
                .and(ProductSpecifications.referenceContains(reference))
                .and(ProductSpecifications.hasCategory(categoryId));
        Page<Product> page = productRepository.findAll(spec, pageable);
        List<ProductResponse> content = page.getContent().stream().map(productMapper::toResponse).toList();
        return PageResponse.of(page, content);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return productMapper.toResponse(getProduct(id));
    }

    // ------------------------------------ Creation ------------------------------------

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsByReference(request.reference())) {
            throw new DuplicateResourceException("Un produit avec cette reference existe deja");
        }
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categorie", request.categoryId()));

        Product product = Product.builder()
                .reference(request.reference())
                .name(request.name())
                .description(request.description())
                .category(category)
                .purchasePrice(request.purchasePrice())
                .salePrice(request.salePrice())
                .build();

        Set<String> seenCells = new HashSet<>();   // (colorId|sizeId) : doublons dans la matrice
        Set<String> seenRefs = new HashSet<>();     // references variantes du lot
        for (VariantCellRequest cell : request.variants()) {
            String cellKey = cell.colorId() + "|" + cell.sizeId();
            if (!seenCells.add(cellKey)) {
                throw new BusinessException(
                        "Variante en double dans la matrice (couleur + taille identiques)", HttpStatus.BAD_REQUEST);
            }
            Color color = colorRepository.findById(cell.colorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Couleur", cell.colorId()));
            Size size = sizeRepository.findById(cell.sizeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Taille", cell.sizeId()));

            String ref = variantSupport.buildVariantReference(request.reference(), size.getLabel(), color.getName());
            if (!seenRefs.add(ref) || productRepository.existsByReference(ref)) {
                throw new DuplicateResourceException("Reference de variante en conflit : " + ref);
            }
            product.addVariant(ProductVariant.builder()
                    .color(color)
                    .size(size)
                    .quantity(cell.quantity())
                    .seuilAlerte(cell.seuilAlerte() != null ? cell.seuilAlerte() : 0)
                    .reference(ref)
                    .qrCode(ref)
                    .build());
        }

        product = productRepository.save(product);
        log.info("Produit cree : {} ({} variante(s))", product.getReference(), product.getVariants().size());
        return productMapper.toResponse(product);
    }

    // -------------------------------- Mise a jour entete --------------------------------

    @Transactional
    public ProductResponse updateHeader(Long id, ProductHeaderRequest request) {
        Product product = getProduct(id);

        boolean referenceChanged = !product.getReference().equalsIgnoreCase(request.reference());
        if (referenceChanged && productRepository.existsByReference(request.reference())) {
            throw new DuplicateResourceException("Un produit avec cette reference existe deja");
        }
        if (!Objects.equals(product.getSalePrice(), request.salePrice())
                || !Objects.equals(product.getPurchasePrice(), request.purchasePrice())) {
            log.warn("[AUDIT] Modification de prix du produit id={}", id);
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categorie", request.categoryId()));

        product.setReference(request.reference());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setCategory(category);
        product.setPurchasePrice(request.purchasePrice());
        product.setSalePrice(request.salePrice());

        // La reference produit change -> on regenere les references/QR des variantes pour rester
        // coherent (REF-SIZE-COLOR). Implique une reimpression des etiquettes QR.
        if (referenceChanged) {
            for (ProductVariant v : product.getVariants()) {
                String newRef = variantSupport.buildVariantReference(
                        request.reference(), v.getSize().getLabel(), v.getColor().getName());
                v.setReference(newRef);
                v.setQrCode(newRef);
            }
        }
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = getProduct(id);
        if (saleItemRepository.existsByVariant_Product_Id(id)) {
            throw new BusinessException(
                    "Impossible de supprimer un produit deja vendu (historique des ventes)", HttpStatus.CONFLICT);
        }
        // Collecte les fichiers AVANT suppression pour les nettoyer du disque (anti-orphelins).
        List<String> urls = product.getImages().stream().map(ProductImage::getUrl).toList();
        productRepository.delete(product); // cascade -> variantes + images (lignes DB)
        urls.forEach(fileStorageService::delete);
        log.warn("[AUDIT] Produit supprime : {} (id={}) — {} image(s) nettoyee(s)",
                product.getReference(), id, urls.size());
    }

    // ---------------------------------- Galerie photos -----------------------------------

    /** Plafond d'images par produit (anti-abus, pages legeres). */
    private static final int MAX_IMAGES = 6;

    /** Attache une ou plusieurs images a un produit (couverture = position 0). */
    @Transactional
    public ProductResponse uploadImages(Long id, List<MultipartFile> files) {
        Product product = getProduct(id);
        if (files == null || files.isEmpty()) {
            throw new BusinessException("Aucun fichier fourni", HttpStatus.BAD_REQUEST);
        }
        int current = product.getImages().size();
        if (current + files.size() > MAX_IMAGES) {
            throw new BusinessException(
                    "Trop d'images : maximum " + MAX_IMAGES + " par produit (actuellement " + current + ")",
                    HttpStatus.BAD_REQUEST);
        }
        files.forEach(fileStorageService::validate);      // pre-validation du lot (anti-orphelin)
        for (MultipartFile file : files) {
            product.addImage(fileStorageService.store(file));
        }
        return productMapper.toResponse(productRepository.save(product));
    }

    /** Retire une image (ligne DB + fichier disque) ; la couverture devient l'image suivante. */
    @Transactional
    public ProductResponse deleteImage(Long productId, Long imageId) {
        Product product = getProduct(productId);
        ProductImage image = product.getImages().stream()
                .filter(i -> i.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Image", imageId));
        String url = image.getUrl();
        product.getImages().remove(image); // orphanRemoval -> DELETE de la ligne
        product.normalizePositions();
        Product saved = productRepository.save(product);
        fileStorageService.delete(url);
        return productMapper.toResponse(saved);
    }

    /** Reordonne la galerie ; la liste doit contenir exactement les images du produit. */
    @Transactional
    public ProductResponse reorderImages(Long productId, List<Long> imageIds) {
        Product product = getProduct(productId);
        List<ProductImage> images = product.getImages();
        Set<Long> provided = new HashSet<>(imageIds);
        Set<Long> existing = images.stream().map(ProductImage::getId).collect(java.util.stream.Collectors.toSet());
        if (imageIds.size() != images.size() || !provided.equals(existing)) {
            throw new BusinessException(
                    "L'ordre fourni doit contenir exactement les images du produit", HttpStatus.BAD_REQUEST);
        }
        Map<Long, ProductImage> byId = images.stream()
                .collect(java.util.stream.Collectors.toMap(ProductImage::getId, java.util.function.Function.identity()));
        for (int i = 0; i < imageIds.size(); i++) {
            byId.get(imageIds.get(i)).setPosition(i);
        }
        return productMapper.toResponse(productRepository.save(product));
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
    }
}
