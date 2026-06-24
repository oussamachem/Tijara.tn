package com.smartboutique.controller;

import com.smartboutique.dto.ChangePasswordRequest;
import com.smartboutique.dto.MessageResponse;
import com.smartboutique.dto.UpdateProfileRequest;
import com.smartboutique.dto.UserResponse;
import com.smartboutique.security.UserPrincipal;
import com.smartboutique.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Gestion du profil de l'utilisateur connecte (ADMIN ou VENDEUR).
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public UserResponse getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.getProfile(principal.getId());
    }

    @PutMapping
    public UserResponse updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.getId(), request);
    }

    @PutMapping("/password")
    public MessageResponse changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                          @Valid @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(principal.getId(), request);
    }
}
