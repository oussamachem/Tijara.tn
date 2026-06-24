package com.smartboutique.mapper;

import com.smartboutique.dto.ReturnResponse;
import com.smartboutique.entity.Return;
import org.springframework.stereotype.Component;

/** Conversion des retours vers leurs DTO. */
@Component
public class ReturnMapper {

    public ReturnResponse toResponse(Return ret) {
        return new ReturnResponse(
                ret.getId(),
                ret.getSale().getId(),
                ret.getProduct().getId(),
                ret.getProduct().getReference(),
                ret.getProduct().getName(),
                ret.getQuantity(),
                ret.getReason(),
                ret.getReturnDate()
        );
    }
}
