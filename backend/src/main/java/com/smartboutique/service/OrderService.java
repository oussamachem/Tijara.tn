package com.smartboutique.service;

import com.smartboutique.dto.OrderCreateRequest;
import com.smartboutique.dto.OrderResponse;
import com.smartboutique.entity.Order;
import com.smartboutique.entity.OrderItem;
import com.smartboutique.entity.ProductVariant;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.repository.OrderRepository;
import com.smartboutique.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Commandes en ligne. Le tenant est deja pose (par le slug, cf. {@link ShopService#enterShop}) :
 * les variantes sont resolues sous RLS (une variante d'une AUTRE boutique est invisible -> 404),
 * les prix sont ceux du tenant du slug (jamais fournis par le client), le stock n'est PAS
 * decremente (C3 : retenu a la confirmation, Phase 5).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;

    @Transactional
    public OrderResponse create(Long clientId, OrderCreateRequest request) {
        Order order = Order.builder().clientId(clientId).reference("TMP-" + UUID.randomUUID()).build();

        BigDecimal total = BigDecimal.ZERO;
        List<OrderCreateRequest.Line> lines = request.items().stream()
                .sorted(Comparator.comparing(OrderCreateRequest.Line::variantId)).toList();
        for (OrderCreateRequest.Line line : lines) {
            // RLS : findById ne renvoie que les variantes du tenant courant (slug) -> pas de
            // commande cross-boutique via un id falsifie.
            ProductVariant v = variantRepository.findById(line.variantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variante", line.variantId()));
            BigDecimal unit = v.getProduct().getSalePrice();   // prix serveur (tenant du slug)
            order.addItem(OrderItem.builder().variant(v).quantity(line.quantity()).unitPrice(unit).build());
            total = total.add(unit.multiply(BigDecimal.valueOf(line.quantity())));
        }
        order.setTotalAmount(total);

        order = orderRepository.save(order);                     // boutique_id via DEFAULT = tenant courant
        order.setReference("CMD-" + String.format("%06d", order.getId()));
        orderRepository.save(order);
        log.info("Commande {} creee (client {}, {} article(s), total {})",
                order.getReference(), clientId, order.getItems().size(), total);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listMine(Long clientId) {
        return orderRepository.findByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(this::toResponse).toList();
    }

    private OrderResponse toResponse(Order o) {
        List<OrderResponse.Item> items = o.getItems().stream()
                .map(it -> new OrderResponse.Item(
                        it.getVariant().getProduct().getName(),
                        it.getVariant().getColor().getName(),
                        it.getVariant().getSize().getLabel(),
                        it.getQuantity(), it.getUnitPrice()))
                .toList();
        return new OrderResponse(o.getId(), o.getReference(), o.getStatus(), o.getTotalAmount(),
                o.getCreatedAt(), items);
    }
}
