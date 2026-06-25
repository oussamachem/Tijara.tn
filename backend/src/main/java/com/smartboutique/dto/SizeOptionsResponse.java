package com.smartboutique.dto;

import com.smartboutique.entity.SizeType;

import java.util.List;

/** Tailles autorisees pour une categorie (selon son SizeType) — alimente le formulaire web. */
public record SizeOptionsResponse(SizeType sizeType, List<String> sizes) {
}
