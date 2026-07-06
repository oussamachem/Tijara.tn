package com.smartboutique.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * Creation d'une reservation (layaway). Le total est calcule serveur (prix captures) ;
 * {@code downPayment} = acompte initial optionnel ; {@code durationDays} override la duree
 * par defaut (30 j). Le mode de l'acompte initial est {@code downPaymentMethod} (defaut ESPECES).
 */
public record ReservationCreateRequest(
        @NotBlank String customerName,
        String customerPhone,
        @NotEmpty @Valid List<ReservationItemRequest> items,
        @PositiveOrZero BigDecimal downPayment,
        String downPaymentMethod,
        Integer durationDays
) {
}
