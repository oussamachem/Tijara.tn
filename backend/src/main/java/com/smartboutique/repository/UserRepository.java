package com.smartboutique.repository;

import com.smartboutique.entity.Role;
import com.smartboutique.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    /** Vendeurs d'UNE boutique (users n'a pas de RLS -> scoping applicatif par tenant). */
    List<User> findByRoleAndBoutiqueId(Role role, Long boutiqueId);
}
