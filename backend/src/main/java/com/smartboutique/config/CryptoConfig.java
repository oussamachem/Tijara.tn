package com.smartboutique.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Beans de cryptographie. L'encodeur vit HORS de {@code SecurityConfig} (qui injecte le
 * {@code JwtAuthenticationFilter}) pour éviter un cycle : le filtre → {@code CustomUserDetailsService}
 * → {@code PasswordEncoder}. En l'isolant ici, la chaîne ne repasse plus par {@code SecurityConfig}.
 */
@Configuration
public class CryptoConfig {

    /** Encodeur BCrypt : hachage/vérification des mots de passe (et placeholder JIT Keycloak). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
