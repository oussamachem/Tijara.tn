package com.smartboutique.service;

import com.smartboutique.dto.OrderAdminDetailResponse;
import com.smartboutique.dto.OrderAdminResponse;
import com.smartboutique.dto.OrderCreateRequest;
import com.smartboutique.dto.OrderResponse;
import com.smartboutique.entity.*;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.notification.OrderStatusChangedEvent;
import com.smartboutique.repository.OrderRepository;
import com.smartboutique.repository.OrderStatusHistoryRepository;
import com.smartboutique.repository.ProductVariantRepository;
import com.smartboutique.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Commandes en ligne. Cote CLIENT (creation, suivi) : le tenant vient du slug. Cote BOUTIQUE-ADMIN
 * (liste, detail, cycle de vie) : le tenant vient du JWT ; la RLS garantit qu'un admin ne voit/agit
 * que sur SES commandes.
 *
 * <p><b>Stock (C3)</b> : decremente UNIQUEMENT a la confirmation (decrement atomique conditionnel,
 * jamais negatif) ; restaure a l'annulation SI deja decremente. Transitions de statut controlees.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;

    // ------------------------------------ CLIENT ------------------------------------

    @Transactional
    public OrderResponse create(Long clientId, OrderCreateRequest request) {
        Order order = Order.builder().clientId(clientId).reference("TMP-" + UUID.randomUUID()).build();

        BigDecimal total = BigDecimal.ZERO;
        List<OrderCreateRequest.Line> lines = request.items().stream()
                .sorted(Comparator.comparing(OrderCreateRequest.Line::variantId)).toList();
        for (OrderCreateRequest.Line line : lines) {
            // RLS : ne resout que les variantes du tenant du slug -> pas de commande cross-boutique.
            ProductVariant v = variantRepository.findById(line.variantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variante", line.variantId()));
            BigDecimal unit = v.getProduct().getSalePrice();   // prix serveur
            order.addItem(OrderItem.builder().variant(v).quantity(line.quantity()).unitPrice(unit).build());
            total = total.add(unit.multiply(BigDecimal.valueOf(line.quantity())));
        }
        order.setTotalAmount(total);

        // Snapshot livraison : figé depuis le profil du client (l'admin l'utilise pour créer le colis).
        User client = userRepository.findById(clientId).orElse(null);
        if (client != null) {
            order.setDeliveryName(client.getFullName());
            order.setDeliveryPhone(client.getPhone());
            order.setDeliveryAddress(client.getAddress());
            order.setDeliveryGovernorat(client.getGovernorat());
        }

        order = orderRepository.save(order);                     // boutique_id via DEFAULT = tenant du slug
        order.setReference("CMD-" + String.format("%06d", order.getId()));
        orderRepository.save(order);

        String clientName = client != null ? client.getFullName() : "Client";
        historyRepository.save(OrderStatusHistory.builder()
                .orderId(order.getId()).fromStatus(null).toStatus(OrderStatus.EN_ATTENTE)
                .changedBy(clientName).build());
        log.info("Commande {} creee (client {}, {} article(s), total {})",
                order.getReference(), clientId, order.getItems().size(), total);
        return toClientResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listMine(Long clientId) {
        return orderRepository.findByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(this::toClientResponse).toList();
    }

    // --------------------------------- BOUTIQUE-ADMIN ---------------------------------

    @Transactional(readOnly = true)
    public List<OrderAdminResponse> listForShop(OrderStatus status, String query) {
        List<Order> orders = (status != null)
                ? orderRepository.findByStatusOrderByCreatedAtDesc(status)
                : orderRepository.findAllWithItems();

        Map<Long, User> clients = userRepository.findAllById(
                orders.stream().map(Order::getClientId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        String term = query != null ? query.trim().toLowerCase() : "";
        return orders.stream()
                .map(o -> toAdminRow(o, clients.get(o.getClientId())))
                .filter(r -> term.isEmpty()
                        || r.reference().toLowerCase().contains(term)
                        || (r.clientName() != null && r.clientName().toLowerCase().contains(term))
                        || (r.clientEmail() != null && r.clientEmail().toLowerCase().contains(term)))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderAdminDetailResponse getDetail(Long orderId) {
        Order order = orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", orderId));
        return toAdminDetail(order);
    }

    /**
     * Fait evoluer le statut (transition controlee). CONFIRMEE -> decrement atomique du stock
     * (409 si insuffisant, rollback total). ANNULEE depuis un statut ou le stock etait decremente
     * -> restauration du stock. Journalise + publie un evenement (notification apres commit).
     */
    @Transactional
    public OrderAdminDetailResponse changeStatus(Long orderId, OrderStatus target, Long actorId) {
        Order order = orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", orderId));
        OrderStatus from = order.getStatus();

        if (from == target) {
            throw new BusinessException("La commande est deja au statut " + target, HttpStatus.BAD_REQUEST);
        }
        if (!from.canTransitionTo(target)) {
            throw new BusinessException(
                    "Transition de statut invalide : " + from + " -> " + target, HttpStatus.CONFLICT);
        }

        if (target == OrderStatus.CONFIRMEE) {
            decrementStock(order);
        } else if (target == OrderStatus.ANNULEE && from.stockDecremented()) {
            restoreStock(order);
        }

        order.setStatus(target);
        orderRepository.save(order);

        String actor = userRepository.findById(actorId).map(User::getFullName).orElse("Boutique");
        historyRepository.save(OrderStatusHistory.builder()
                .orderId(orderId).fromStatus(from).toStatus(target).changedBy(actor).build());

        User client = userRepository.findById(order.getClientId()).orElse(null);
        events.publishEvent(new OrderStatusChangedEvent(orderId, order.getReference(), order.getClientId(),
                client != null ? client.getEmail() : null, from, target));
        log.info("[AUDIT] Commande {} : {} -> {} par {}", order.getReference(), from, target, actor);
        return toAdminDetail(order);
    }

    // ---------------------------------- Stock ----------------------------------

    /** Decrement atomique conditionnel par variante (anti-survente, jamais negatif). */
    private void decrementStock(Order order) {
        List<OrderItem> items = order.getItems().stream()
                .sorted(Comparator.comparing(it -> it.getVariant().getId())).toList();
        for (OrderItem it : items) {
            int updated = variantRepository.decrementStockIfAvailable(it.getVariant().getId(), it.getQuantity());
            if (updated == 0) {
                ProductVariant v = it.getVariant();
                throw new BusinessException(
                        "Stock insuffisant pour '" + v.getReference() + "' (" + v.getColor().getName()
                                + " / " + v.getSize().getLabel() + ") : demande " + it.getQuantity()
                                + ", disponible " + v.getQuantity(), HttpStatus.CONFLICT);
            }
        }
    }

    /** Restaure le stock (annulation apres decrement). */
    private void restoreStock(Order order) {
        for (OrderItem it : order.getItems()) {
            variantRepository.incrementStock(it.getVariant().getId(), it.getQuantity());
        }
    }

    // ---------------------------------- Mappers ----------------------------------

    private OrderResponse toClientResponse(Order o) {
        return new OrderResponse(o.getId(), o.getReference(), o.getStatus(), o.getTotalAmount(),
                o.getCreatedAt(), itemsOf(o));
    }

    private OrderAdminResponse toAdminRow(Order o, User client) {
        return new OrderAdminResponse(o.getId(), o.getReference(), o.getStatus(), o.getTotalAmount(),
                o.getCreatedAt(),
                client != null ? client.getFullName() : null,
                client != null ? client.getEmail() : null,
                o.getItems().size());
    }

    private OrderAdminDetailResponse toAdminDetail(Order o) {
        User client = userRepository.findById(o.getClientId()).orElse(null);
        List<OrderAdminDetailResponse.StatusEvent> history = historyRepository
                .findByOrderIdOrderByCreatedAtAsc(o.getId()).stream()
                .map(h -> new OrderAdminDetailResponse.StatusEvent(
                        h.getFromStatus(), h.getToStatus(), h.getChangedBy(), h.getCreatedAt()))
                .toList();
        return new OrderAdminDetailResponse(o.getId(), o.getReference(), o.getStatus(), o.getTotalAmount(),
                o.getCreatedAt(),
                client != null ? client.getFullName() : null,
                client != null ? client.getEmail() : null,
                o.getDeliveryName(), o.getDeliveryPhone(), o.getDeliveryAddress(), o.getDeliveryGovernorat(),
                o.getCarrierEan(), o.getCarrierStatus(), o.getCarrierStatusAt(),
                itemsOf(o), history);
    }

    private List<OrderResponse.Item> itemsOf(Order o) {
        return o.getItems().stream()
                .map(it -> new OrderResponse.Item(
                        it.getVariant().getProduct().getName(),
                        it.getVariant().getColor().getName(),
                        it.getVariant().getSize().getLabel(),
                        it.getQuantity(), it.getUnitPrice()))
                .toList();
    }
}
