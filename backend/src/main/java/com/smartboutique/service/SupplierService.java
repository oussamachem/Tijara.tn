package com.smartboutique.service;

import com.smartboutique.dto.SupplierRequest;
import com.smartboutique.dto.SupplierResponse;
import com.smartboutique.entity.Supplier;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.mapper.DebtMapper;
import com.smartboutique.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestion des fournisseurs (pour les dettes / comptes a payer). */
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final DebtMapper debtMapper;

    @Transactional(readOnly = true)
    public List<SupplierResponse> findAll() {
        return supplierRepository.findAllByOrderByNameAsc().stream()
                .map(debtMapper::toSupplierResponse).toList();
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        Supplier s = Supplier.builder()
                .name(request.name().trim()).phone(request.phone()).address(request.address()).build();
        return debtMapper.toSupplierResponse(supplierRepository.save(s));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
        s.setName(request.name().trim());
        s.setPhone(request.phone());
        s.setAddress(request.address());
        return debtMapper.toSupplierResponse(supplierRepository.save(s));
    }
}
