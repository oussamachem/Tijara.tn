package com.smartboutique.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notifications client au changement de statut de commande. ARCHITECTURE PREPAREE : le listener
 * est declenche APRES commit (TransactionPhase.AFTER_COMMIT) -> on ne notifie jamais un changement
 * qui aurait ete rollback. Aujourd'hui = log (simulation). Pour activer un canal reel (email/push/
 * SMS), il suffit d'injecter le service d'envoi ici, sans toucher au code metier.
 */
@Slf4j
@Component
public class OrderNotificationListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(OrderStatusChangedEvent e) {
        // TODO(notification) : remplacer ce log par un envoi reel (email/push) au client.
        log.info("[NOTIF] Commande {} : {} -> {} — notifier le client {} ({})",
                e.reference(), e.from(), e.to(), e.clientId(), e.clientEmail());
    }
}
