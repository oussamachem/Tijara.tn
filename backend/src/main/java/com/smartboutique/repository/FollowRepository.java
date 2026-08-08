package com.smartboutique.repository;

import com.smartboutique.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByUserIdAndShopId(Long userId, Long shopId);

    void deleteByUserIdAndShopId(Long userId, Long shopId);

    List<Follow> findByUserId(Long userId);

    long countByShopId(Long shopId);
}
