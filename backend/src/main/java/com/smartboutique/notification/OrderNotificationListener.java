package com.smartboutique.notification;

import com.smartboutique.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notifications client au changement de statut de commande. Declenche APRES commit
 * (TransactionPhase.AFTER_COMMIT) -> on ne notifie jamais un changement rollback. Cree une
 * notification IN-APP (persistee, consultable par le client). Le canal externe (email/push/SMS)
 * pourra etre branche ici sans toucher au code metier.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(OrderStatusChangedEvent e) {
        notificationService.notifyOrderStatus(e.clientId(), e.reference(), e.to(), e.orderId());
        log.info("[NOTIF] Commande {} : {} -> {} — notification in-app creee pour le client {} ({})",
                e.reference(), e.from(), e.to(), e.clientId(), e.clientEmail());
    }
}
