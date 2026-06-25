package com.smartboutique.controller;

import com.smartboutique.dto.ColorResponse;
import com.smartboutique.service.ColorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lecture du catalogue de couleurs (necessaire au formulaire de creation produit). */
@RestController
@RequestMapping("/api/colors")
@RequiredArgsConstructor
public class ColorController {

    private final ColorService colorService;

    @GetMapping
    public List<ColorResponse> list() {
        return colorService.findAll();
    }
}
