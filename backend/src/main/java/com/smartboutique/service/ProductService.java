package com.smartboutique.service;

import com.smartboutique.dto.PageResponse;
import com.smartboutique.dto.ProductRequest;
import com.smartboutique.dto.ProductResponse;
import com.smartboutique.entity.Category;
import com.smartboutique.entity.Product;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.DuplicateResourceException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.mapper.ProductMapper;
import com.smartboutique.repository.CategoryRepository;
import com.smartboutique.repository.ProductRepository;
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

import java.util.List;
import java.util.Objects;

/**
 * Gestion des produits : CRUD, recherche paginee, generation du QR Code et image associee.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final QrCodeService qrCodeService;
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

    /** Recherche un produit par le contenu de son QR Code (la reference) — pour la vente par scan. */
    @Transactional(readOnly = true)
    public ProductResponse findByQrContent(String content) {
        Product product = productRepository.findByQrCode(content)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun produit ne correspond a ce QR Code (" + content + ")"));
        return productMapper.toResponse(product);
    }

    /** Genere l'image PNG du QR Code du produit (a partir de son contenu encode). */
    @Transactional(readOnly = true)
    public byte[] generateQrPng(Long id) {
        Product product = getProduct(id);
        return qrCodeService.generatePng(product.getQrCode());
    }

    // -------------------------------------- Ecriture -------------------------------------

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsByReference(request.reference())) {
            throw new DuplicateResourceException("Un produit avec cette reference existe deja");
        }
        Product product = Product.builder()
                .reference(request.reference())
                .name(request.name())
                .description(request.description())
                .category(resolveCategory(request.categoryId()))
                .size(request.size())
                .color(request.color())
                .purchasePrice(request.purchasePrice())
                .salePrice(request.salePrice())
                .quantity(request.quantity())
                .seuilAlerte(request.seuilAlerte())
                // QR Code : on encode la reference (stable et signifiante) plutot que l'id auto-incremente.
                .qrCode(request.reference())
                .build();

        product = productRepository.save(product);
        log.info("Produit cree : {} (reference={}, qrCode={})",
                product.getName(), product.getReference(), product.getQrCode());
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);

        boolean referenceChanged = !product.getReference().equalsIgnoreCase(request.reference());
        if (referenceChanged && productRepository.existsByReference(request.reference())) {
            throw new DuplicateResourceException("Un produit avec cette reference existe deja");
        }

        // Tracabilite : journalise toute modification de prix (action sensible).
        if (!Objects.equals(product.getSalePrice(), request.salePrice())
                || !Objects.equals(product.getPurchasePrice(), request.purchasePrice())) {
            log.warn("[AUDIT] Modification de prix du produit id={} : achat {}->{}, vente {}->{}",
                    id, product.getPurchasePrice(), request.purchasePrice(),
                    product.getSalePrice(), request.salePrice());
        }

        product.setReference(request.reference());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setCategory(resolveCategory(request.categoryId()));
        product.setSize(request.size());
        product.setColor(request.color());
        product.setPurchasePrice(request.purchasePrice());
        product.setSalePrice(request.salePrice());
        product.setQuantity(request.quantity());
        product.setSeuilAlerte(request.seuilAlerte());
        // Le QR Code suit la reference si celle-ci change.
        if (referenceChanged) {
            product.setQrCode(request.reference());
        }

        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = getProduct(id);
        productRepository.delete(product);
        log.warn("[AUDIT] Produit supprime : {} (id={}, reference={})",
                product.getName(), id, product.getReference());
    }

    /** Associe une image uploadee au produit. */
    @Transactional
    public ProductResponse uploadImage(Long id, MultipartFile file) {
        Product product = getProduct(id);
        String url = fileStorageService.store(file);
        product.setImageUrl(url);
        return productMapper.toResponse(productRepository.save(product));
    }

    // ---------------------------------------- Stock --------------------------------------

    /** Definit la quantite absolue en stock (operation d'inventaire ADMIN). */
    @Transactional
    public ProductResponse setStock(Long id, int quantity) {
        Product product = getProduct(id);
        int before = product.getQuantity();
        product.setQuantity(quantity);
        Product saved = productRepository.save(product);
        log.info("[AUDIT] Stock du produit id={} fixe : {} -> {}", id, before, quantity);
        return productMapper.toResponse(saved);
    }

    /** Ajuste le stock d'une valeur relative ; refuse un resultat negatif. */
    @Transactional
    public ProductResponse adjustStock(Long id, int delta) {
        Product product = getProduct(id);
        int result = product.getQuantity() + delta;
        if (result < 0) {
            throw new BusinessException(
                    "Ajustement impossible : le stock deviendrait negatif (actuel "
                            + product.getQuantity() + ", variation " + delta + ")",
                    HttpStatus.BAD_REQUEST);
        }
        product.setQuantity(result);
        Product saved = productRepository.save(product);
        log.info("[AUDIT] Stock du produit id={} ajuste de {} : {} -> {}",
                id, delta, result - delta, result);
        return productMapper.toResponse(saved);
    }

    /** Produits en rupture (quantite = 0) ou sous le seuil d'alerte (quantite <= seuil). */
    @Transactional(readOnly = true)
    public List<ProductResponse> findLowStock() {
        return productRepository.findAll(ProductSpecifications.lowStockOnly()).stream()
                .map(productMapper::toResponse)
                .toList();
    }

    // --------------------------------------- Helpers -------------------------------------

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categorie", categoryId));
    }
}
