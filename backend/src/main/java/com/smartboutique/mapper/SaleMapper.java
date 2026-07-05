package com.smartboutique.mapper;

import com.smartboutique.dto.SaleItemResponse;
import com.smartboutique.dto.SalePaymentResponse;
import com.smartboutique.dto.SaleResponse;
import com.smartboutique.entity.Sale;
import com.smartboutique.entity.SaleItem;
import com.smartboutique.entity.SalePayment;
import com.smartboutique.entity.TenderMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Conversion des ventes vers leurs DTO (attributs variante figes a la vente). */
@Component
public class SaleMapper {

    public SaleResponse toResponse(Sale sale) {
        List<SaleItemResponse> items = sale.getItems().stream().map(this::toItemResponse).toList();
        BigDecimal subtotal = items.stream()
                .map(SaleItemResponse::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SalePaymentResponse> payments = sale.getPayments().stream().map(this::toPaymentResponse).toList();

        return new SaleResponse(
                sale.getId(),
                sale.getSeller() != null ? sale.getSeller().getId() : null,
                sale.getSeller() != null ? sale.getSeller().getFullName() : null,
                sale.getSaleDate(),
                sale.getPaymentMethod(),
                subtotal,
                sale.getDiscount(),
                sale.getTotalAmount(),
                items,
                payments,
                computeChange(sale)
        );
    }

    private SalePaymentResponse toPaymentResponse(SalePayment p) {
        return new SalePaymentResponse(p.getMethod(), p.getAmount(), p.getIssuer(),
                p.getTicketCode(), p.getTicketSerial(), p.getTicketExpiry());
    }

    /**
     * Rendu monnaie = uniquement sur la part ESPECES (A2 : pas de rendu sur ticket ; le surplus
     * de tickets est perdu). change = especes - max(0, total - tickets - carte).
     */
    private BigDecimal computeChange(Sale sale) {
        BigDecimal total = sale.getTotalAmount();
        BigDecimal cash = sumByMethod(sale, TenderMethod.ESPECES);
        BigDecimal card = sumByMethod(sale, TenderMethod.CARTE);
        BigDecimal tickets = sumByMethod(sale, TenderMethod.TICKET_CADEAU);
        BigDecimal remainingForCash = total.subtract(tickets).subtract(card).max(BigDecimal.ZERO);
        return cash.subtract(remainingForCash).max(BigDecimal.ZERO);
    }

    private BigDecimal sumByMethod(Sale sale, TenderMethod method) {
        return sale.getPayments().stream()
                .filter(p -> p.getMethod() == method)
                .map(SalePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public SaleItemResponse toItemResponse(SaleItem item) {
        return new SaleItemResponse(
                item.getId(),
                item.getVariant() != null ? item.getVariant().getId() : null,
                item.getVariantReference(),
                item.getProductName(),
                item.getColorName(),
                item.getSize(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
        );
    }
}
