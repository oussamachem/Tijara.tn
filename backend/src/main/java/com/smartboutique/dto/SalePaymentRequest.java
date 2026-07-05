package com.smartboutique.dto;

import com.smartboutique.entity.TenderMethod;
import com.smartboutique.entity.TicketIssuer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Une ligne de paiement (tender). Pour un TICKET_CADEAU : issuer + ticketCode obligatoires,
 * amount ∈ denominations autorisees, ticketExpiry (si fournie) non depassee (valide serveur).
 */
public record SalePaymentRequest(
        @NotNull(message = "Le mode de paiement de la ligne est obligatoire")
        TenderMethod method,

        @NotNull(message = "Le montant est obligatoire")
        @Positive(message = "Le montant doit etre strictement positif")
        BigDecimal amount,

        TicketIssuer issuer,
        String ticketCode,
        String ticketSerial,
        LocalDate ticketExpiry
) {
}
