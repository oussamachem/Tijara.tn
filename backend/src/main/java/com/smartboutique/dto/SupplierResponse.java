package com.smartboutique.dto;

/** Representation d'un fournisseur. */
public record SupplierResponse(
        Long id,
        String name,
        String phone,
        String address
) {
}
