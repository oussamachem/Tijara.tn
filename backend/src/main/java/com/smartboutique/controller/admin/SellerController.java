package com.smartboutique.controller.admin;

import com.smartboutique.dto.CreateSellerRequest;
import com.smartboutique.dto.UpdateSellerRequest;
import com.smartboutique.dto.UserResponse;
import com.smartboutique.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD des vendeurs, reserve a l'ADMIN (route sous /api/admin/**).
 */
@RestController
@RequestMapping("/api/admin/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final UserService userService;

    @GetMapping
    public List<UserResponse> list() {
        return userService.listSellers();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.getSeller(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateSellerRequest request) {
        return userService.createSeller(request);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateSellerRequest request) {
        return userService.updateSeller(id, request);
    }

    /** Desactive le compte vendeur (active=false). */
    @PatchMapping("/{id}/deactivate")
    public UserResponse deactivate(@PathVariable Long id) {
        return userService.setSellerActive(id, false);
    }

    /** Reactive le compte vendeur (active=true). */
    @PatchMapping("/{id}/activate")
    public UserResponse activate(@PathVariable Long id) {
        return userService.setSellerActive(id, true);
    }
}
