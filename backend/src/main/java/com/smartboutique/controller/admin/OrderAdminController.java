package com.smartboutique.controller.admin;

import com.smartboutique.dto.ChangeStatusRequest;
import com.smartboutique.dto.OrderAdminDetailResponse;
import com.smartboutique.dto.OrderAdminResponse;
import com.smartboutique.entity.OrderStatus;
import com.smartboutique.security.UserPrincipal;
import com.smartboutique.service.GoodexService;
import com.smartboutique.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion des commandes en ligne cote BOUTIQUE-ADMIN (route sous /api/admin/** -> ADMIN).
 * Toutes les operations sont scopees au tenant courant par la RLS.
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderService orderService;
    private final GoodexService goodexService;

    /** Liste des commandes de la boutique (filtre statut + recherche reference/client). */
    @GetMapping
    public List<OrderAdminResponse> list(@RequestParam(required = false) OrderStatus status,
                                         @RequestParam(required = false) String query) {
        return orderService.listForShop(status, query);
    }

    @GetMapping("/{id}")
    public OrderAdminDetailResponse get(@PathVariable Long id) {
        return orderService.getDetail(id);
    }

    /** Fait evoluer le statut (transition controlee ; stock gere automatiquement). */
    @PostMapping("/{id}/status")
    public OrderAdminDetailResponse changeStatus(@PathVariable Long id,
                                                 @Valid @RequestBody ChangeStatusRequest request,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return orderService.changeStatus(id, request.status(), principal.getId());
    }

    /** Crée le colis Goodex (transporteur) pour la commande et renvoie le détail (avec le code de suivi). */
    @PostMapping("/{id}/ship")
    public OrderAdminDetailResponse ship(@PathVariable Long id) {
        goodexService.createColis(id);
        return orderService.getDetail(id);
    }

    /** Rafraîchit le statut Goodex du colis de la commande. */
    @PostMapping("/{id}/tracking")
    public OrderAdminDetailResponse tracking(@PathVariable Long id) {
        goodexService.refreshStatus(id);
        return orderService.getDetail(id);
    }
}
