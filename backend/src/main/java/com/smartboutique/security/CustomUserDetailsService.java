package com.smartboutique.security;

import com.smartboutique.entity.User;
import com.smartboutique.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Charge un utilisateur par email pour l'authentification. Phase A : login = IDENTITE seule
 * (aucune boutique). Une boutique suspendue bloque l'ACCES a cette boutique (filtre X-Shop-Id),
 * pas la connexion : un user reste libre de naviguer comme client / dans ses autres boutiques.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Identifiants invalides"));
        return new UserPrincipal(user);
    }
}
