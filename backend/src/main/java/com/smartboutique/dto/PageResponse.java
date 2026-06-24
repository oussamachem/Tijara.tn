package com.smartboutique.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Enveloppe de pagination stable (evite de serialiser directement PageImpl de Spring).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    /** Construit une reponse paginee a partir d'une Page Spring et de son contenu deja mappe. */
    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
