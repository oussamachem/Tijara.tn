package com.smartboutique.service;

import com.smartboutique.dto.NotificationResponse;
import com.smartboutique.entity.Notification;
import com.smartboutique.entity.OrderStatus;
import com.smartboutique.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Notifications in-app. La creation liee a une commande est appelee APRES commit du changement de
 * statut (cf. OrderNotificationListener) -> ouvre sa propre transaction. Table globale (hors RLS).
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    /**
     * Cree une notification client pour un changement de statut (si pertinent). Appelee depuis un
     * listener AFTER_COMMIT -> REQUIRES_NEW obligatoire : sans transaction NEUVE, l'INSERT ne serait
     * jamais committe (la transaction d'origine est deja terminee).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyOrderStatus(Long clientId, String reference, OrderStatus to, Long orderId) {
        String title, body;
        switch (to) {
            case CONFIRMEE -> {
                title = "Commande " + reference + " confirmee";
                body = "Votre commande est confirmee, la boutique la prepare.";
            }
            case PRETE -> {
                title = "Commande " + reference + " prete";
                body = "Votre commande est prete a etre recuperee en boutique.";
            }
            case RECUPEREE -> {
                title = "Commande " + reference + " recuperee";
                body = "Merci ! Votre commande a bien ete remise.";
            }
            case ANNULEE -> {
                title = "Commande " + reference + " annulee";
                body = "Votre commande a ete annulee.";
            }
            default -> {
                return; // EN_ATTENTE (creation) : rien a notifier
            }
        }
        repository.save(Notification.builder()
                .userId(clientId).type("ORDER_STATUS").title(title).body(body).orderId(orderId).read(false)
                .build());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Long userId) {
        return repository.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    /** Marque une notification comme lue (idempotent, verrouille au proprietaire). */
    @Transactional
    public void markRead(Long id, Long userId) {
        repository.findByIdAndUserId(id, userId).ifPresent(n -> n.setRead(true));
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repository.markAllRead(userId);
    }
}
