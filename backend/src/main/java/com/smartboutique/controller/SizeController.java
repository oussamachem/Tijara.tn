package com.smartboutique.controller;

import com.smartboutique.dto.SizeResponse;
import com.smartboutique.service.SizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lecture du catalogue de tailles, accessible a tout utilisateur authentifie
 * (necessaire au formulaire produit cote web).
 */
@RestController
@RequestMapping("/api/sizes")
@RequiredArgsConstructor
public class SizeController {

    private final SizeService sizeService;

    @GetMapping
    public List<SizeResponse> list() {
        return sizeService.findAll();
    }

    @GetMapping("/{id}")
    public SizeResponse get(@PathVariable Long id) {
        return sizeService.findById(id);
    }
}
