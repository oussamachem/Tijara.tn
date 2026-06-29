package com.smartboutique.service;

import com.smartboutique.dto.CustomerRequest;
import com.smartboutique.dto.CustomerResponse;
import com.smartboutique.entity.Customer;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.mapper.CreditMapper;
import com.smartboutique.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestion des clients (pour les ventes a credit). */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CreditMapper creditMapper;

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customerRepository.findAllByOrderByNameAsc().stream()
                .map(creditMapper::toCustomerResponse).toList();
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        Customer c = Customer.builder()
                .name(request.name().trim())
                .phone(request.phone())
                .address(request.address())
                .build();
        return creditMapper.toCustomerResponse(customerRepository.save(c));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        c.setName(request.name().trim());
        c.setPhone(request.phone());
        c.setAddress(request.address());
        return creditMapper.toCustomerResponse(customerRepository.save(c));
    }
}
