package com.smartboutique.controller;

import com.smartboutique.dto.ReturnRequest;
import com.smartboutique.dto.ReturnResponse;
import com.smartboutique.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Retours produits, accessibles a l'ADMIN et au VENDEUR.
 */
@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnResponse create(@Valid @RequestBody ReturnRequest request) {
        return returnService.createReturn(request);
    }
}
