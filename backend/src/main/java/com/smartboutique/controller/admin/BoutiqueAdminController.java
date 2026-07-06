package com.smartboutique.controller.admin;

import com.smartboutique.dto.BoutiqueResponse;
import com.smartboutique.dto.CreateBoutiqueRequest;
import com.smartboutique.service.BoutiqueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Espace PLATEFORME (SUPER_ADMIN uniquement) : gestion des boutiques (tenants).
 * Route protegee par SecurityConfig (/api/admin/boutiques/** -> hasRole SUPER_ADMIN).
 */
@RestController
@RequestMapping("/api/admin/boutiques")
@RequiredArgsConstructor
public class BoutiqueAdminController {

    private final BoutiqueService boutiqueService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BoutiqueResponse create(@Valid @RequestBody CreateBoutiqueRequest request) {
        return boutiqueService.create(request);
    }

    @GetMapping
    public List<BoutiqueResponse> list() {
        return boutiqueService.list();
    }

    @PostMapping("/{id}/suspend")
    public BoutiqueResponse suspend(@PathVariable Long id) {
        return boutiqueService.suspend(id);
    }

    @PostMapping("/{id}/reactivate")
    public BoutiqueResponse reactivate(@PathVariable Long id) {
        return boutiqueService.reactivate(id);
    }
}
