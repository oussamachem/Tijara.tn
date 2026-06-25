package com.smartboutique.controller.admin;

import com.smartboutique.dto.StockAdjustRequest;
import com.smartboutique.dto.StockSetRequest;
import com.smartboutique.dto.VariantResponse;
import com.smartboutique.service.VariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Operations ADMIN au grain VARIANTE : suppression, stock (inventaire / ajustement).
 */
@RestController
@RequestMapping("/api/admin/variants")
@RequiredArgsConstructor
public class VariantAdminController {

    private final VariantService variantService;

    /** Retire une variante (refuse si derniere du produit ou deja vendue). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        variantService.removeVariant(id);
    }

    /** Definit la quantite absolue en stock de la variante (inventaire). */
    @PatchMapping("/{id}/stock")
    public VariantResponse setStock(@PathVariable Long id, @Valid @RequestBody StockSetRequest request) {
        return variantService.setStock(id, request.quantity());
    }

    /** Ajuste le stock de la variante d'une valeur relative. */
    @PatchMapping("/{id}/stock/adjust")
    public VariantResponse adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustRequest request) {
        return variantService.adjustStock(id, request.delta());
    }
}
