package com.smartboutique.controller.admin;

import com.smartboutique.dto.*;
import com.smartboutique.security.UserPrincipal;
import com.smartboutique.service.CreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Gestion des ventes a credit, reservee a l'ADMIN (route sous /api/admin/**). */
@RestController
@RequestMapping("/api/admin/credits")
@RequiredArgsConstructor
public class CreditAdminController {

    private final CreditService creditService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreditResponse create(@Valid @RequestBody CreditCreateRequest request,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return creditService.create(request, principal.getId());
    }

    @GetMapping
    public PageResponse<CreditSummaryResponse> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        return creditService.search(customerId, status, from, to, pageable);
    }

    /** Tableau de bord credits (tresorerie). Chemin litteral prioritaire sur /{id}. */
    @GetMapping("/dashboard")
    public CreditDashboardResponse dashboard() {
        return creditService.dashboard();
    }

    @GetMapping("/{id}")
    public CreditResponse get(@PathVariable Long id) {
        return creditService.findById(id);
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public CreditResponse pay(@PathVariable Long id, @Valid @RequestBody CreditPaymentRequest request) {
        return creditService.addPayment(id, request);
    }

    @PostMapping("/{id}/cancel")
    public CreditResponse cancel(@PathVariable Long id) {
        return creditService.cancel(id);
    }
}
