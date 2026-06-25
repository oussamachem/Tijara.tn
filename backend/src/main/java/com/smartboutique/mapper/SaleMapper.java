package com.smartboutique.mapper;

import com.smartboutique.dto.SaleItemResponse;
import com.smartboutique.dto.SaleResponse;
import com.smartboutique.entity.Sale;
import com.smartboutique.entity.SaleItem;
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

        return new SaleResponse(
                sale.getId(),
                sale.getSeller() != null ? sale.getSeller().getId() : null,
                sale.getSeller() != null ? sale.getSeller().getFullName() : null,
                sale.getSaleDate(),
                sale.getPaymentMethod(),
                subtotal,
                sale.getDiscount(),
                sale.getTotalAmount(),
                items
        );
    }

    private SaleItemResponse toItemResponse(SaleItem item) {
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
