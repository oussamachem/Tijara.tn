package com.smartboutique.dto;

import com.smartboutique.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * Demande de creation d'une vente.
 * {@code discount} = remise en MONTANT FIXE (meme devise) soustraite du sous-total ; optionnelle (0 par defaut).
 */
public record SaleRequest(
        @NotEmpty(message = "La vente doit contenir au moins un article")
        @Valid
        List<SaleItemRequest> items,

        @NotNull(message = "Le mode de paiement est obligatoire")
        PaymentMethod paymentMethod,

        @PositiveOrZero(message = "La remise ne peut etre negative")
        BigDecimal discount
) {
}
