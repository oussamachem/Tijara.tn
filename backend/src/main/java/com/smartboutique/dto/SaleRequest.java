package com.smartboutique.dto;

import com.smartboutique.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * Demande de creation d'une vente.
 * {@code discount} = remise en MONTANT FIXE soustraite du sous-total ; optionnelle (0 par defaut).
 *
 * <p>Paiement : soit {@code paymentMethod} (mode unique, retro-compatible), soit {@code payments}
 * (liste de tenders : especes/carte/ticket cadeau, combinables). Au moins l'un des deux.</p>
 */
public record SaleRequest(
        @NotEmpty(message = "La vente doit contenir au moins un article")
        @Valid
        List<SaleItemRequest> items,

        /** Mode unique (retro-compatible). Ignore si {@code payments} est fourni. */
        PaymentMethod paymentMethod,

        @PositiveOrZero(message = "La remise ne peut etre negative")
        BigDecimal discount,

        /** Lignes de paiement (tenders). Prioritaire sur {@code paymentMethod} si non vide. */
        @Valid
        List<SalePaymentRequest> payments
) {
}
