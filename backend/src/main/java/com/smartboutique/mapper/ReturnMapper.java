package com.smartboutique.mapper;

import com.smartboutique.dto.ReturnResponse;
import com.smartboutique.entity.ProductVariant;
import com.smartboutique.entity.Return;
import org.springframework.stereotype.Component;

/** Conversion des retours vers leurs DTO (au grain variante). */
@Component
public class ReturnMapper {

    public ReturnResponse toResponse(Return ret) {
        ProductVariant v = ret.getVariant();
        return new ReturnResponse(
                ret.getId(),
                ret.getSale().getId(),
                v.getId(),
                v.getReference(),
                v.getProduct().getName(),
                v.getColor() != null ? v.getColor().getName() : null,
                v.getSize().getLabel(),
                ret.getQuantity(),
                ret.getReason(),
                ret.getReturnDate()
        );
    }
}
