package com.smartboutique.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Validation des tokens Keycloak (OIDC). Le {@link JwtDecoder} vit dans une configuration DÉDIÉE
 * (pas dans {@code SecurityConfig}) pour éviter un cycle de beans : {@code SecurityConfig} injecte
 * le {@code JwtAuthenticationFilter}, qui dépend lui-même de ce décodeur.
 *
 * <p>Le JWKS est chargé PARESSEUSEMENT (à la 1re requête, pas au démarrage) → le backend démarre
 * même si Keycloak n'est pas encore prêt. Chaque token est validé : signature (clé JWKS du realm),
 * issuer attendu et expiration.</p>
 */
@Configuration
public class KeycloakConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${app.keycloak.jwk-set-uri}") String jwkSetUri,
            @Value("${app.keycloak.issuer}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }
}
