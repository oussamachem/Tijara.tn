package com.smartboutique.dto;

import com.smartboutique.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Creation d'une vente A CREDIT : produits vendus (-> stock decremente, prix captures),
 * client (existant via {@code customerId} OU nouveau via {@code newCustomer}), acompte initial
 * et echeance. Le TOTAL n'est PAS saisi : il derive des lignes.
 */
public record CreditCreateRequest(
        Long customerId,

        @Valid
        CustomerRequest newCustomer,

        @NotEmpty(message = "Le credit doit contenir au moins un article")
        @Valid
        List<SaleItemRequest> items,

        /** Mode de paiement du sous-jacent / de l'acompte (defaut ESPECES). */
        PaymentMethod paymentMethod,

        @PositiveOrZero(message = "La remise ne peut etre negative")
        BigDecimal discount,

        /** Acompte verse a la creation (0 par defaut). Ne peut depasser le total. */
        @PositiveOrZero(message = "L'acompte ne peut etre negatif")
        BigDecimal downPayment,

        LocalDate dueDate
) {
}
