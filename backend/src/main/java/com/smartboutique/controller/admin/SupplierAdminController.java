package com.smartboutique.controller.admin;

import com.smartboutique.dto.SupplierRequest;
import com.smartboutique.dto.SupplierResponse;
import com.smartboutique.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gestion des fournisseurs, reservee a l'ADMIN. */
@RestController
@RequestMapping("/api/admin/suppliers")
@RequiredArgsConstructor
public class SupplierAdminController {

    private final SupplierService supplierService;

    @GetMapping
    public List<SupplierResponse> list() {
        return supplierService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@Valid @RequestBody SupplierRequest request) {
        return supplierService.create(request);
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return supplierService.update(id, request);
    }
}
