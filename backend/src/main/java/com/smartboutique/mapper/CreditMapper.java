package com.smartboutique.mapper;

import com.smartboutique.dto.*;
import com.smartboutique.entity.Credit;
import com.smartboutique.entity.CreditPayment;
import com.smartboutique.entity.CreditStatus;
import com.smartboutique.entity.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Conversion des credits/clients vers leurs DTO. Reste et statut DERIVES (total - paye). */
@Component
@RequiredArgsConstructor
public class CreditMapper {

    private final SaleMapper saleMapper;

    public CustomerResponse toCustomerResponse(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getPhone(), c.getAddress());
    }

    public CreditPaymentResponse toPaymentResponse(CreditPayment p) {
        return new CreditPaymentResponse(p.getId(), p.getAmount(), p.getMethod(), p.getCreatedAt());
    }

    /** Detail complet (lignes + paiements). {@code paid} = somme des paiements (calculee au service). */
    public CreditResponse toResponse(Credit credit, BigDecimal paid) {
        BigDecimal total = credit.getSale().getTotalAmount();
        BigDecimal remaining = total.subtract(paid);
        List<SaleItemResponse> items = credit.getSale().getItems().stream()
                .map(saleMapper::toItemResponse).toList();
        List<CreditPaymentResponse> payments = credit.getPayments().stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(this::toPaymentResponse).toList();
        return new CreditResponse(
                credit.getId(),
                credit.getSale().getId(),
                toCustomerResponse(credit.getCustomer()),
                total,
                paid,
                remaining,
                CreditStatus.of(total, paid, credit.isCancelled()),
                credit.getDueDate(),
                credit.isCancelled(),
                credit.getCreatedAt(),
                items,
                payments);
    }

    /** Ligne de liste depuis la projection SQL (paye deja agrege). */
    public CreditSummaryResponse toSummary(CreditRow row) {
        BigDecimal remaining = row.total().subtract(row.paid());
        return new CreditSummaryResponse(
                row.id(),
                row.customerId(),
                row.customerName(),
                row.total(),
                row.paid(),
                remaining,
                CreditStatus.of(row.total(), row.paid(), row.cancelled()),
                row.dueDate(),
                row.cancelled(),
                row.createdAt());
    }
}
