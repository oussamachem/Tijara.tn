package com.smartboutique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Creation / modification d'un fournisseur. */
public record SupplierRequest(
        @NotBlank(message = "Le nom du fournisseur est obligatoire")
        String name,

        @Size(max = 50, message = "Le telephone ne peut depasser 50 caracteres")
        String phone,

        @Size(max = 500, message = "L'adresse ne peut depasser 500 caracteres")
        String address
) {
}
