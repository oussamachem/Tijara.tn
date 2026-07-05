package com.smartboutique.dto;

import com.smartboutique.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Vente complete avec ses lignes. */
public record SaleResponse(
        Long id,
        Long sellerId,
        String sellerName,
        LocalDateTime saleDate,
        PaymentMethod paymentMethod,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal totalAmount,
        List<SaleItemResponse> items,
        // Lignes de paiement (tenders) ; le rendu monnaie ne se calcule que sur la part especes.
        List<SalePaymentResponse> payments,
        BigDecimal change
) {
}
