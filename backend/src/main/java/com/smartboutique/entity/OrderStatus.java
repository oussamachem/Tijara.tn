package com.smartboutique.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Cycle de vie d'une commande en ligne (C4) et regles de transition :
 * <pre>
 *   EN_ATTENTE --confirmer--> CONFIRMEE (stock decremente)
 *   CONFIRMEE  --preparer---> PRETE
 *   PRETE      --remettre---> RECUPEREE (terminal)
 *   * --annuler--> ANNULEE (terminal ; stock rendu si deja decremente)
 * </pre>
 */
public enum OrderStatus {
    EN_ATTENTE,
    CONFIRMEE,
    PRETE,
    RECUPEREE,
    ANNULEE;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            EN_ATTENTE, EnumSet.of(CONFIRMEE, ANNULEE),
            CONFIRMEE, EnumSet.of(PRETE, ANNULEE),
            PRETE, EnumSet.of(RECUPEREE, ANNULEE),
            RECUPEREE, EnumSet.noneOf(OrderStatus.class),
            ANNULEE, EnumSet.noneOf(OrderStatus.class));

    /** La transition de CE statut vers {@code target} est-elle autorisee ? */
    public boolean canTransitionTo(OrderStatus target) {
        return ALLOWED.get(this).contains(target);
    }

    /** Le stock est-il DEJA decremente dans ce statut ? (base de la restauration a l'annulation). */
    public boolean stockDecremented() {
        return this == CONFIRMEE || this == PRETE;
    }
}
