package com.smartboutique.controller.admin;

import com.smartboutique.dto.PageResponse;
import com.smartboutique.dto.ReturnResponse;
import com.smartboutique.dto.SaleSummaryResponse;
import com.smartboutique.service.ReturnService;
import com.smartboutique.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Historique des ventes et retours, reserve a l'ADMIN (route sous /api/admin/**).
 * Dates {@code from}/{@code to} au format ISO (yyyy-MM-dd), incluses, optionnelles.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class HistoryController {

    private final SaleService saleService;
    private final ReturnService returnService;

    /** Historique des ventes filtre par periode et/ou vendeur. */
    @GetMapping("/sales")
    public PageResponse<SaleSummaryResponse> sales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long sellerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return saleService.searchHistory(from, to, sellerId, pageable);
    }

    /** Historique des retours filtre par periode. */
    @GetMapping("/returns")
    public PageResponse<ReturnResponse> returns(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        return returnService.searchReturns(from, to, pageable);
    }
}
