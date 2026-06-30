package com.smartboutique.controller.admin;

import com.smartboutique.dto.*;
import com.smartboutique.service.SupplierDebtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Dettes fournisseurs (comptes a payer), reservees a l'ADMIN. */
@RestController
@RequestMapping("/api/admin/debts")
@RequiredArgsConstructor
public class SupplierDebtAdminController {

    private final SupplierDebtService debtService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DebtResponse create(@Valid @RequestBody DebtCreateRequest request) {
        return debtService.create(request);
    }

    @GetMapping
    public PageResponse<DebtSummaryResponse> list(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        return debtService.search(supplierId, status, from, to, pageable);
    }

    @GetMapping("/dashboard")
    public DebtDashboardResponse dashboard() {
        return debtService.dashboard();
    }

    @GetMapping("/{id}")
    public DebtResponse get(@PathVariable Long id) {
        return debtService.findById(id);
    }

    @PutMapping("/{id}")
    public DebtResponse update(@PathVariable Long id, @Valid @RequestBody DebtUpdateRequest request) {
        return debtService.update(id, request);
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public DebtResponse pay(@PathVariable Long id, @Valid @RequestBody DebtPaymentRequest request) {
        return debtService.addPayment(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        debtService.delete(id);
    }
}
