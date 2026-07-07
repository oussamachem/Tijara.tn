package com.smartboutique.dto;

import com.smartboutique.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

/** Changement de statut d'une commande (cote boutique-admin). */
public record ChangeStatusRequest(@NotNull OrderStatus status) {
}
