package com.smartboutique.controller;

import com.smartboutique.dto.PageResponse;
import com.smartboutique.dto.SaleRequest;
import com.smartboutique.dto.SaleResponse;
import com.smartboutique.dto.SaleSummaryResponse;
import com.smartboutique.security.UserPrincipal;
import com.smartboutique.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Ventes, accessibles a l'ADMIN et au VENDEUR (route hors /api/admin).
 */
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    /** Enregistre une vente ; le vendeur est l'utilisateur connecte. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse create(@Valid @RequestBody SaleRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        return saleService.createSale(request, principal.getId());
    }

    /**
     * Historique pagine des ventes de l'utilisateur CONNECTE (vendeur ou admin).
     * Le vendeur est deduit du token : un VENDEUR ne voit que ses propres ventes
     * (sert notamment a choisir une vente a retourner cote mobile).
     */
    @GetMapping("/mine")
    public PageResponse<SaleSummaryResponse> myHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        return saleService.searchHistory(from, to, principal.getId(), pageable);
    }

    @GetMapping("/{id}")
    public SaleResponse get(@PathVariable Long id) {
        return saleService.findById(id);
    }
}
