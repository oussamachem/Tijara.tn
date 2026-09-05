package com.smartboutique.security;

import com.smartboutique.entity.User;
import com.smartboutique.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Résolution de l'utilisateur applicatif à partir de l'identité Keycloak (par email).
 * L'identité vient de Keycloak (OIDC) ; l'utilisateur app porte l'{@code id} référencé partout
 * (shop_members, commandes, favoris, RLS). Le mot de passe app n'est plus utilisé (auth = Keycloak).
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Compat : chargement par email (throw si absent). Utilisé par l'AuthenticationManager legacy. */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Identifiants invalides"));
        return new UserPrincipal(user);
    }

    /**
     * JIT provisioning : retrouve l'utilisateur app par email (identité Keycloak) ou le CRÉE.
     * Préserve les comptes existants (même email -> mêmes boutiques/commandes/favoris). Le mot de
     * passe app est un placeholder aléatoire (non utilisable : la connexion passe par Keycloak).
     */
    @Transactional
    public UserPrincipal loadOrCreateByEmail(String email, String fullName, boolean platformAdmin) {
        User user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(User.builder()
                .email(email)
                .fullName(fullName != null && !fullName.isBlank() ? fullName : email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString())) // jamais utilisé
                .platformAdmin(platformAdmin)
                .active(true)
                .build()));
        return new UserPrincipal(user);
    }
}
