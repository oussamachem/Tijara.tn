package com.smartboutique.controller.admin;

import com.smartboutique.dto.ShopResponse;
import com.smartboutique.service.BoutiqueService;
import com.smartboutique.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Reglages de MA boutique (PROPRIETAIRE, sous /api/admin/** -> SHOP_OWNER) : logo / photo de profil.
 * La boutique cible est la boutique ACTIVE (X-Shop-Id validee -> TenantContext).
 */
@RestController
@RequestMapping("/api/admin/shop")
@RequiredArgsConstructor
public class ShopSettingsController {

    private final BoutiqueService boutiqueService;

    /** Fiche de ma boutique (nom, slug, logo courant). */
    @GetMapping
    public ShopResponse myShop() {
        return boutiqueService.getShop(TenantContext.get());
    }

    /** Definit / remplace le logo de ma boutique. */
    @PostMapping("/logo")
    public ShopResponse uploadLogo(@RequestParam("file") MultipartFile file) {
        return boutiqueService.updateLogo(TenantContext.get(), file);
    }

    /** Retire le logo de ma boutique. */
    @DeleteMapping("/logo")
    public ShopResponse removeLogo() {
        return boutiqueService.removeLogo(TenantContext.get());
    }
}
