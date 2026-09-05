package com.smartboutique.repository;

import com.smartboutique.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    /** Ids des produits mis en favori par l'utilisateur (les plus récents d'abord). */
    @Query("select f.productId from Favorite f where f.userId = :userId order by f.createdAt desc")
    List<Long> findProductIdsByUserId(Long userId);
}
