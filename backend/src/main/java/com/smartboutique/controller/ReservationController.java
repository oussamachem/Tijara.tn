package com.smartboutique.controller;

import com.smartboutique.dto.ReservationCreateRequest;
import com.smartboutique.dto.ReservationPaymentRequest;
import com.smartboutique.dto.ReservationResponse;
import com.smartboutique.dto.ReservationSummaryResponse;
import com.smartboutique.security.UserPrincipal;
import com.smartboutique.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Reservations (layaway), accessibles a l'ADMIN et au VENDEUR (route hors /api/admin) :
 * le vendeur cree/paie/consulte, l'admin voit tout.
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /** Cree une reservation (stock retenu) ; le vendeur est l'utilisateur connecte. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@Valid @RequestBody ReservationCreateRequest request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return reservationService.create(request, principal.getId());
    }

    /** Liste (filtre optionnel par statut) avec reste et jours restants derives. */
    @GetMapping
    public List<ReservationSummaryResponse> list(@RequestParam(required = false) String status) {
        return reservationService.list(status);
    }

    /** Echeance proche (alerte vendeur). Chemin litteral prioritaire sur /{id}. */
    @GetMapping("/due-soon")
    public List<ReservationSummaryResponse> dueSoon() {
        return reservationService.dueSoon();
    }

    @GetMapping("/{id}")
    public ReservationResponse get(@PathVariable Long id) {
        return reservationService.findById(id);
    }

    /** Ajoute un versement ; le vendeur encaisseur est l'utilisateur connecte. Solde -> cloture. */
    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse pay(@PathVariable Long id,
                                   @Valid @RequestBody ReservationPaymentRequest request,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return reservationService.addPayment(id, request, principal.getId());
    }

    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable Long id) {
        return reservationService.cancel(id);
    }
}
