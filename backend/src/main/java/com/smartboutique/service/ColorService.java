package com.smartboutique.service;

import com.smartboutique.dto.ColorRequest;
import com.smartboutique.dto.ColorResponse;
import com.smartboutique.entity.Color;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.DuplicateResourceException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.mapper.ColorMapper;
import com.smartboutique.repository.ColorRepository;
import com.smartboutique.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestion du catalogue de couleurs (ADMIN). */
@Service
@RequiredArgsConstructor
public class ColorService {

    private final ColorRepository colorRepository;
    private final ProductVariantRepository variantRepository;
    private final ColorMapper colorMapper;

    @Transactional(readOnly = true)
    public List<ColorResponse> findAll() {
        return colorRepository.findAll().stream().map(colorMapper::toResponse).toList();
    }

    @Transactional
    public ColorResponse create(ColorRequest request) {
        if (colorRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Une couleur portant ce nom existe deja");
        }
        Color color = colorRepository.save(Color.builder().name(request.name()).hex(request.hex()).build());
        return colorMapper.toResponse(color);
    }

    @Transactional
    public ColorResponse update(Long id, ColorRequest request) {
        Color color = getColor(id);
        if (!color.getName().equalsIgnoreCase(request.name()) && colorRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Une couleur portant ce nom existe deja");
        }
        color.setName(request.name());
        color.setHex(request.hex());
        return colorMapper.toResponse(colorRepository.save(color));
    }

    @Transactional
    public void delete(Long id) {
        Color color = getColor(id);
        if (variantRepository.existsByColorId(id)) {
            throw new BusinessException(
                    "Impossible de supprimer cette couleur : des variantes l'utilisent",
                    HttpStatus.CONFLICT);
        }
        colorRepository.delete(color);
    }

    private Color getColor(Long id) {
        return colorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Couleur", id));
    }
}
