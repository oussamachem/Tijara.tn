package com.smartboutique.mapper;

import com.smartboutique.dto.ColorResponse;
import com.smartboutique.entity.Color;
import org.springframework.stereotype.Component;

@Component
public class ColorMapper {

    public ColorResponse toResponse(Color color) {
        return new ColorResponse(color.getId(), color.getName(), color.getHex());
    }
}
