package com.smartboutique.controller.admin;

import com.smartboutique.dto.SizeRequest;
import com.smartboutique.dto.SizeResponse;
import com.smartboutique.service.SizeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * CRUD du catalogue de tailles, reserve a l'ADMIN (route sous /api/admin/**).
 */
@RestController
@RequestMapping("/api/admin/sizes")
@RequiredArgsConstructor
public class SizeAdminController {

    private final SizeService sizeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SizeResponse create(@Valid @RequestBody SizeRequest request) {
        return sizeService.create(request);
    }

    @PutMapping("/{id}")
    public SizeResponse update(@PathVariable Long id, @Valid @RequestBody SizeRequest request) {
        return sizeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        sizeService.delete(id);
    }
}
