package com.smartboutique.security;

import com.smartboutique.entity.BoutiqueStatus;
import com.smartboutique.entity.User;
import com.smartboutique.repository.BoutiqueRepository;
import com.smartboutique.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Charge un utilisateur par son email pour l'authentification Spring Security. Une boutique
 * SUSPENDED rend ses utilisateurs non authentifiables (compte considere desactive).
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final BoutiqueRepository boutiqueRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Identifiants invalides"));
        // Un user rattache a une boutique suspendue ne peut pas se connecter (le SUPER_ADMIN
        // plateforme reste sur la boutique par defaut, active).
        boolean boutiqueActive = user.getBoutiqueId() == null
                || boutiqueRepository.findById(user.getBoutiqueId())
                        .map(b -> b.getStatus() != BoutiqueStatus.SUSPENDED)
                        .orElse(true);
        return new UserPrincipal(user, boutiqueActive);
    }
}
