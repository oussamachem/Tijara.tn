package com.smartboutique.service;

import com.smartboutique.dto.SizeRequest;
import com.smartboutique.dto.SizeResponse;
import com.smartboutique.entity.Size;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.DuplicateResourceException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.mapper.SizeMapper;
import com.smartboutique.repository.ProductVariantRepository;
import com.smartboutique.repository.SizeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion du catalogue de tailles (calque de {@link CategoryService}).
 * Unicite du libelle insensible a la casse et au trim ; suppression refusee (409)
 * si des produits referencent la taille.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SizeService {

    private final SizeRepository sizeRepository;
    private final ProductVariantRepository variantRepository;
    private final SizeMapper sizeMapper;

    @Transactional(readOnly = true)
    public List<SizeResponse> findAll() {
        return sizeRepository.findAllOrdered().stream().map(sizeMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SizeResponse findById(Long id) {
        return sizeMapper.toResponse(getSize(id));
    }

    @Transactional
    public SizeResponse create(SizeRequest request) {
        String label = request.label().trim();
        if (sizeRepository.existsByLabelIgnoreCase(label)) {
            throw new DuplicateResourceException("Une taille portant ce libelle existe deja");
        }
        Size size = Size.builder()
                .label(label)
                .position(request.position())
                .build();
        return sizeMapper.toResponse(sizeRepository.save(size));
    }

    @Transactional
    public SizeResponse update(Long id, SizeRequest request) {
        Size size = getSize(id);
        String label = request.label().trim();
        // Verifie l'unicite (insensible a la casse) seulement si le libelle change reellement.
        if (!size.getLabel().equalsIgnoreCase(label)
                && sizeRepository.existsByLabelIgnoreCase(label)) {
            throw new DuplicateResourceException("Une taille portant ce libelle existe deja");
        }
        size.setLabel(label);
        size.setPosition(request.position());
        return sizeMapper.toResponse(sizeRepository.save(size));
    }

    /** Supprime une taille, sauf si des variantes la referencent encore (409). */
    @Transactional
    public void delete(Long id) {
        Size size = getSize(id);
        if (variantRepository.existsBySizeId(id)) {
            throw new BusinessException(
                    "Impossible de supprimer cette taille : des variantes y sont rattachees",
                    HttpStatus.CONFLICT);
        }
        sizeRepository.delete(size);
        log.info("Taille supprimee : {} (id={})", size.getLabel(), id);
    }

    private Size getSize(Long id) {
        return sizeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Taille", id));
    }
}
