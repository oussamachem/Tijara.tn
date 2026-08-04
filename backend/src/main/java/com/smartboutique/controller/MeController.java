package com.smartboutique.controller;

import com.smartboutique.dto.MembershipResponse;
import com.smartboutique.dto.ShopResponse;
import com.smartboutique.entity.Boutique;
import com.smartboutique.entity.ShopMember;
import com.smartboutique.repository.BoutiqueRepository;
import com.smartboutique.repository.ShopMemberRepository;
import com.smartboutique.security.UserPrincipal;
import com.smartboutique.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Espace IDENTITE de l'utilisateur connecte (hors contexte boutique) : sert au front unifie a
 * aiguiller vers l'espace OWNER / VENDOR / CLIENT et a alimenter le selecteur de boutique.
 *
 * <p>Route deliberement au grain IDENTITE : accessible a tout utilisateur AUTHENTIFIE (y compris
 * un client sans boutique), donc SANS exiger le header X-Shop-Id. C'est de la LECTURE des
 * memberships existants — pas le self-service de creation de boutique (Phase B).</p>
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final ShopMemberRepository shopMemberRepository;
    private final BoutiqueRepository boutiqueRepository;
    private final FollowService followService;

    /** Boutiques SUIVIES par l'utilisateur connecté (« Mes boutiques » de l'accueil). */
    @GetMapping("/follows")
    public List<ShopResponse> myFollows(@AuthenticationPrincipal UserPrincipal principal) {
        return followService.myFollows(principal.getId());
    }

    /** Boutiques dont l'utilisateur connecte est membre, avec son role contextuel (OWNER/VENDOR). */
    @GetMapping("/shops")
    public List<MembershipResponse> myShops(@AuthenticationPrincipal UserPrincipal principal) {
        List<ShopMember> memberships = shopMemberRepository.findByUserId(principal.getId());
        if (memberships.isEmpty()) {
            return List.of();
        }
        // Chargement groupe des boutiques (evite N+1) puis projection.
        Map<Long, Boutique> shops = boutiqueRepository
                .findAllById(memberships.stream().map(ShopMember::getShopId).toList())
                .stream().collect(Collectors.toMap(Boutique::getId, Function.identity()));

        return memberships.stream()
                .map(m -> {
                    Boutique b = shops.get(m.getShopId());
                    return b == null ? null : new MembershipResponse(
                            b.getId(), b.getName(), b.getSlug(), m.getRole(), b.getStatus());
                })
                .filter(java.util.Objects::nonNull)
                // OWNER avant VENDOR, puis par nom : la boutique dont on est proprietaire d'abord.
                .sorted(Comparator.comparing(MembershipResponse::role).thenComparing(MembershipResponse::name))
                .toList();
    }
}
