package com.smartboutique.dto;

/** Representation d'un client. */
public record CustomerResponse(
        Long id,
        String name,
        String phone,
        String address
) {
}
