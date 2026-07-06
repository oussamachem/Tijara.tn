package com.smartboutique.service;

import com.smartboutique.dto.*;
import com.smartboutique.entity.Role;
import com.smartboutique.entity.User;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.DuplicateResourceException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.mapper.UserMapper;
import com.smartboutique.repository.UserRepository;
import com.smartboutique.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion des utilisateurs : profil de l'utilisateur connecte et administration des vendeurs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    // ----------------------- Profil de l'utilisateur connecte -----------------------

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return userMapper.toResponse(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        // Verifie l'unicite de l'email si modifie.
        if (!user.getEmail().equalsIgnoreCase(request.email())
                && userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Cet email est deja utilise");
        }
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public MessageResponse changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException("L'ancien mot de passe est incorrect", HttpStatus.BAD_REQUEST);
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return new MessageResponse("Mot de passe modifie avec succes.");
    }

    // ----------------------------- Gestion des vendeurs -----------------------------

    @Transactional(readOnly = true)
    public List<UserResponse> listSellers() {
        // users n'a pas de RLS (login pre-tenant) -> on scope explicitement a la boutique courante.
        // tenant null (hors requete authentifiee : tests, taches) -> pas de restriction.
        Long tenant = TenantContext.get();
        List<User> sellers = (tenant == null)
                ? userRepository.findByRole(Role.VENDEUR)
                : userRepository.findByRoleAndBoutiqueId(Role.VENDEUR, tenant);
        return sellers.stream().map(userMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getSeller(Long id) {
        return userMapper.toResponse(findSeller(id));
    }

    @Transactional
    public UserResponse createSeller(CreateSellerRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Cet email est deja utilise");
        }
        User seller = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.VENDEUR)
                .active(true)
                .boutiqueId(TenantContext.get())   // rattache le vendeur a la boutique de l'admin
                .build();
        seller = userRepository.save(seller);
        log.info("Vendeur cree : {} (id={})", seller.getEmail(), seller.getId());
        return userMapper.toResponse(seller);
    }

    @Transactional
    public UserResponse updateSeller(Long id, UpdateSellerRequest request) {
        User seller = findSeller(id);
        if (!seller.getEmail().equalsIgnoreCase(request.email())
                && userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Cet email est deja utilise");
        }
        seller.setFullName(request.fullName());
        seller.setEmail(request.email());
        return userMapper.toResponse(userRepository.save(seller));
    }

    /** Desactivation d'un vendeur (action sensible -> tracee). */
    @Transactional
    public UserResponse setSellerActive(Long id, boolean active) {
        User seller = findSeller(id);
        seller.setActive(active);
        seller = userRepository.save(seller);
        log.warn("[AUDIT] Compte vendeur {} {} (id={})",
                seller.getEmail(), active ? "REACTIVE" : "DESACTIVE", seller.getId());
        return userMapper.toResponse(seller);
    }

    // --------------------------------- Helpers ---------------------------------

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
    }

    private User findSeller(Long id) {
        User user = findUser(id);
        // Scoping tenant : un admin ne voit/modifie QUE les vendeurs de SA boutique (sinon 404,
        // pas de fuite). users n'ayant pas de RLS, ce controle est fait cote application.
        Long tenant = TenantContext.get();
        if (user.getRole() != Role.VENDEUR || (tenant != null && !tenant.equals(user.getBoutiqueId()))) {
            throw new ResourceNotFoundException("Vendeur", id);
        }
        return user;
    }
}
