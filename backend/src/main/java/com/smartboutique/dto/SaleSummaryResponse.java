package com.smartboutique.dto;

import com.smartboutique.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Resume d'une vente pour la liste d'historique (sans les lignes, pour eviter le N+1). */
public record SaleSummaryResponse(
        Long id,
        Long sellerId,
        String sellerName,
        LocalDateTime saleDate,
        PaymentMethod paymentMethod,
        BigDecimal discount,
        BigDecimal totalAmount,
        Long itemCount
) {
}
