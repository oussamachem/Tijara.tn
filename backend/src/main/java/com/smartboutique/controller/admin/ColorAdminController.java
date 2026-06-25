package com.smartboutique.controller.admin;

import com.smartboutique.dto.ColorRequest;
import com.smartboutique.dto.ColorResponse;
import com.smartboutique.service.ColorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** CRUD du catalogue de couleurs, reserve a l'ADMIN. */
@RestController
@RequestMapping("/api/admin/colors")
@RequiredArgsConstructor
public class ColorAdminController {

    private final ColorService colorService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ColorResponse create(@Valid @RequestBody ColorRequest request) {
        return colorService.create(request);
    }

    @PutMapping("/{id}")
    public ColorResponse update(@PathVariable Long id, @Valid @RequestBody ColorRequest request) {
        return colorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        colorService.delete(id);
    }
}
