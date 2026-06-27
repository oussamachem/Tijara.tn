package com.smartboutique.mapper;

import com.smartboutique.dto.SizeResponse;
import com.smartboutique.entity.Size;
import org.springframework.stereotype.Component;

/** Conversion entre l'entite Size et ses DTO. */
@Component
public class SizeMapper {

    public SizeResponse toResponse(Size size) {
        return new SizeResponse(
                size.getId(),
                size.getLabel(),
                size.getPosition()
        );
    }
}
