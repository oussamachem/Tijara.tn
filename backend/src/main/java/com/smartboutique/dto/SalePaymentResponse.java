package com.smartboutique.dto;

import com.smartboutique.entity.TenderMethod;
import com.smartboutique.entity.TicketIssuer;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Une ligne de paiement figee sur la vente (tracabilite ticket cadeau incluse). */
public record SalePaymentResponse(
        TenderMethod method,
        BigDecimal amount,
        TicketIssuer issuer,
        String ticketCode,
        String ticketSerial,
        LocalDate ticketExpiry
) {
}
