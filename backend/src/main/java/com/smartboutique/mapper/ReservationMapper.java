package com.smartboutique.mapper;

import com.smartboutique.dto.*;
import com.smartboutique.entity.Reservation;
import com.smartboutique.entity.ReservationItem;
import com.smartboutique.entity.ReservationPayment;
import com.smartboutique.entity.ReservationStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/** Conversion des reservations vers leurs DTO. Reste et jours restants DERIVES (serveur). */
@Component
public class ReservationMapper {

    /** Seuil d'alerte "echeance proche" (B5, defaut 4 jours). */
    @Value("${app.reservations.reminder-days:4}")
    private int reminderDays;

    /** Jours restants avant l'echeance (peut etre negatif si depasse, avant passage du job). */
    private long daysRemaining(LocalDateTime dueDate) {
        return ChronoUnit.DAYS.between(LocalDateTime.now(), dueDate);
    }

    public ReservationItemResponse toItemResponse(ReservationItem it) {
        BigDecimal linePrice = it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity()));
        return new ReservationItemResponse(
                it.getVariant().getId(), it.getVariantReference(), it.getProductName(),
                it.getColorName(), it.getSize(), it.getQuantity(), it.getUnitPrice(), linePrice);
    }

    public ReservationPaymentResponse toPaymentResponse(ReservationPayment p) {
        return new ReservationPaymentResponse(
                p.getId(), p.getAmount(), p.getMethod(), p.getSeller().getFullName(), p.getCreatedAt());
    }

    /** Detail complet. {@code paid} = somme des versements (calculee au service). */
    public ReservationResponse toResponse(Reservation r, BigDecimal paid) {
        BigDecimal remaining = r.getTotalAmount().subtract(paid);
        List<ReservationItemResponse> items = r.getItems().stream().map(this::toItemResponse).toList();
        List<ReservationPaymentResponse> payments = r.getPayments().stream()
                .sorted(Comparator.comparing(ReservationPayment::getCreatedAt))
                .map(this::toPaymentResponse).toList();
        return new ReservationResponse(
                r.getId(), r.getReference(), r.getCustomerName(), r.getCustomerPhone(),
                r.getSeller().getFullName(), r.getStatus(),
                r.getTotalAmount(), paid, remaining, daysRemaining(r.getDueDate()),
                r.getCreatedAt(), r.getDueDate(), r.getClosedAt(),
                r.isDepositForfeited(),
                r.getSale() != null ? r.getSale().getId() : null,
                items, payments);
    }

    /** Ligne de liste depuis la projection SQL (paye deja agrege). */
    public ReservationSummaryResponse toSummary(ReservationRow row) {
        BigDecimal remaining = row.total().subtract(row.paid());
        long days = daysRemaining(row.dueDate());
        boolean dueSoon = row.status() == ReservationStatus.ACTIVE && days <= reminderDays;
        return new ReservationSummaryResponse(
                row.id(), row.reference(), row.customerName(), row.customerPhone(), row.status(),
                row.total(), row.paid(), remaining, days, dueSoon, row.depositForfeited(),
                row.dueDate(), row.createdAt());
    }
}
