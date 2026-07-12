package com.smartboutique.repository;

import com.smartboutique.entity.ShopMember;
import com.smartboutique.entity.ShopMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopMemberRepository extends JpaRepository<ShopMember, Long> {

    /** Membership d'un user dans une boutique (source de verite de l'autorisation X-Shop-Id). */
    Optional<ShopMember> findByShopIdAndUserId(Long shopId, Long userId);

    /** Boutiques dont le user est membre (pour le futur selecteur GET /api/me/shops). */
    List<ShopMember> findByUserId(Long userId);

    /** Membres d'une boutique par role (ex. lister les vendeurs). */
    List<ShopMember> findByShopIdAndRole(Long shopId, ShopMemberRole role);

    boolean existsByShopIdAndUserId(Long shopId, Long userId);
}
