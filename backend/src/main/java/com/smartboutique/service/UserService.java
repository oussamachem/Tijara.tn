package com.smartboutique.service;

import com.smartboutique.dto.*;
import com.smartboutique.entity.ShopMember;
import com.smartboutique.entity.ShopMemberRole;
import com.smartboutique.entity.User;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.DuplicateResourceException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.mapper.UserMapper;
import com.smartboutique.repository.ShopMemberRepository;
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
 * Gestion des utilisateurs : profil de l'utilisateur connecte et administration des vendeurs de la
 * BOUTIQUE ACTIVE. Phase A : le vendeur est un compte + une membership VENDOR (shop_members) ; le
 * scoping se fait par la boutique active (X-Shop-Id -> TenantContext), garanti par la garde OWNER.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ShopMemberRepository shopMemberRepository;
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
        if (!user.getEmail().equalsIgnoreCase(request.email())
                && userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Cet email est deja utilise");
        }
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(trimToNull(request.phone()));
        user.setAddress(trimToNull(request.address()));
        user.setGovernorat(trimToNull(request.governorat()));
        return userMapper.toResponse(userRepository.save(user));
    }

    /** Vide -> null pour les coordonnées de livraison optionnelles. */
    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
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
        Long shopId = TenantContext.get();
        if (shopId == null) return List.of();
        List<Long> ids = shopMemberRepository.findByShopIdAndRole(shopId, ShopMemberRole.VENDOR)
                .stream().map(ShopMember::getUserId).toList();
        return userRepository.findAllById(ids).stream().map(userMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getSeller(Long id) {
        return userMapper.toResponse(findSeller(id));
    }

    @Transactional
    public UserResponse createSeller(CreateSellerRequest request) {
        Long shopId = requireShop();
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Cet email est deja utilise");
        }
        User seller = userRepository.save(User.builder()
                .fullName(request.fullName())
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .active(true)
                .platformAdmin(false)
                .build());
        // Membership VENDOR sur la boutique active (le vendeur n'a acces qu'a cette boutique).
        shopMemberRepository.save(ShopMember.builder()
                .shopId(shopId).userId(seller.getId()).role(ShopMemberRole.VENDOR).build());
        log.info("Vendeur cree : {} (id={}) sur la boutique {}", seller.getEmail(), seller.getId(), shopId);
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

    private Long requireShop() {
        Long shopId = TenantContext.get();
        if (shopId == null) throw new BusinessException("Aucune boutique active", HttpStatus.BAD_REQUEST);
        return shopId;
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
    }

    /** Un vendeur = membership VENDOR sur la boutique ACTIVE. Sinon 404 (pas de fuite inter-boutiques). */
    private User findSeller(Long id) {
        Long shopId = TenantContext.get();
        boolean vendorHere = shopId != null && shopMemberRepository.findByShopIdAndUserId(shopId, id)
                .map(m -> m.getRole() == ShopMemberRole.VENDOR).orElse(false);
        if (!vendorHere) {
            throw new ResourceNotFoundException("Vendeur", id);
        }
        return findUser(id);
    }
}
