package com.smartboutique.support;

import com.smartboutique.entity.*;
import com.smartboutique.repository.CategoryRepository;
import com.smartboutique.repository.ColorRepository;
import com.smartboutique.repository.ProductRepository;
import com.smartboutique.repository.ProductVariantRepository;

import java.math.BigDecimal;

/** Helpers de creation de donnees pour les tests (au grain variante). */
public final class Fixtures {

    private Fixtures() {
    }

    public static Category category(CategoryRepository repo, String name, SizeType type) {
        return repo.findByName(name).orElseGet(() -> repo.save(
                Category.builder().name(name).sizeType(type).build()));
    }

    public static Color color(ColorRepository repo, String name) {
        return repo.findByName(name).orElseGet(() -> repo.save(Color.builder().name(name).build()));
    }

    /** Cree un produit + UNE variante et renvoie la variante (sauvegardee). */
    public static ProductVariant variant(ProductRepository products, ProductVariantRepository variants,
                                         Category cat, Color color, String ref,
                                         String size, int qty, int seuil, String salePrice) {
        Product p = products.save(Product.builder()
                .reference(ref).name(ref).category(cat)
                .purchasePrice(new BigDecimal("5.00")).salePrice(new BigDecimal(salePrice))
                .build());
        String vref = ref + "-" + size.toUpperCase() + "-" + color.getName().toUpperCase();
        return variants.save(ProductVariant.builder()
                .product(p).color(color).size(size).quantity(qty).seuilAlerte(seuil)
                .reference(vref).qrCode(vref).build());
    }
}
