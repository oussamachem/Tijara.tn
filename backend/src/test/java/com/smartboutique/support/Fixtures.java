package com.smartboutique.support;

import com.smartboutique.entity.*;
import com.smartboutique.repository.CategoryRepository;
import com.smartboutique.repository.ColorRepository;
import com.smartboutique.repository.ProductRepository;
import com.smartboutique.repository.ProductVariantRepository;
import com.smartboutique.repository.SizeRepository;

import java.math.BigDecimal;

/** Helpers de creation de donnees pour les tests (au grain variante, tailles gerees). */
public final class Fixtures {

    private Fixtures() {
    }

    public static Category category(CategoryRepository repo, String name) {
        return repo.findByName(name).orElseGet(() -> repo.save(Category.builder().name(name).build()));
    }

    public static Color color(ColorRepository repo, String name) {
        return repo.findByName(name).orElseGet(() -> repo.save(Color.builder().name(name).build()));
    }

    public static Size size(SizeRepository repo, String label) {
        return repo.findByLabelIgnoreCase(label).orElseGet(() -> repo.save(Size.builder().label(label).build()));
    }

    /** Cree un produit + UNE variante (taille resolue/creee depuis le catalogue) et la renvoie. */
    public static ProductVariant variant(ProductRepository products, ProductVariantRepository variants,
                                         SizeRepository sizes, Category cat, Color color, String ref,
                                         String sizeLabel, int qty, int seuil, String salePrice) {
        Size size = size(sizes, sizeLabel);
        Product p = products.save(Product.builder()
                .reference(ref).name(ref).category(cat)
                .purchasePrice(new BigDecimal("5.00")).salePrice(new BigDecimal(salePrice))
                .build());
        String vref = ref + "-" + size.getLabel().toUpperCase() + "-" + color.getName().toUpperCase();
        return variants.save(ProductVariant.builder()
                .product(p).color(color).size(size).quantity(qty).seuilAlerte(seuil)
                .reference(vref).qrCode(vref).build());
    }
}
