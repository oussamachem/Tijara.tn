package com.smartboutique.config;

import com.smartboutique.security.JwtAuthenticationFilter;
import com.smartboutique.security.RestAccessDeniedHandler;
import com.smartboutique.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration de la securite : chaine de filtres stateless, autorisations par role,
 * CORS, et reponses JSON unifiees pour les erreurs 401/403.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    /** Origines autorisees pour le CORS (web + mobile), externalisees. */
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                // API REST stateless : aucune session HTTP cote serveur.
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics d'authentification + inscription CLIENT (marketplace).
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password").permitAll()
                        // Images produits servies statiquement (affichage <img> cote web).
                        .requestMatchers("/uploads/**").permitAll()
                        // Sonde de sante (healthcheck Docker) + info. Le reste d'Actuator reste protege.
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        // Marketplace : passer commande = tout user AUTHENTIFIE (compte global, P1).
                        // (Declare AVANT le catalogue public ; le tenant vient du slug, pas de X-Shop-Id.)
                        .requestMatchers("/api/shops/*/orders", "/api/shops/*/orders/**").authenticated()
                        // Annuaire + catalogue = PUBLIC (lecture, resolution du tenant par le slug).
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/shops", "/api/shops/**").permitAll()
                        // Espace PLATEFORME : moderation des boutiques reservee a l'admin plateforme.
                        // (Plus specifique -> declare AVANT la regle generale /api/admin/**.)
                        .requestMatchers("/api/admin/boutiques/**").hasRole("PLATFORM_ADMIN")
                        // Back-office d'UNE boutique : reserve au PROPRIETAIRE (OWNER) de la boutique active.
                        .requestMatchers("/api/admin/**").hasRole("SHOP_OWNER")
                        // Tableau de bord : proprietaire de la boutique active.
                        .requestMatchers("/api/dashboard").hasRole("SHOP_OWNER")
                        // Endpoints internes (produits, ventes, stock...) : OWNER ou VENDOR de la boutique
                        // active (autorite posee par le filtre X-Shop-Id apres validation du membership).
                        .anyRequest().hasAnyRole("SHOP_OWNER", "SHOP_VENDOR"))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * AuthenticationManager auto-configure : Spring construit un DaoAuthenticationProvider
     * a partir du bean {@code CustomUserDetailsService} (UserDetailsService) et du
     * {@code PasswordEncoder} ci-dessous. Le statut "actif" est verifie via UserDetails.isEnabled().
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /** Encodeur BCrypt utilise pour le hachage et la verification des mots de passe. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // setAllowedOriginPatterns (et non setAllowedOrigins) : accepte les MOTIFS avec '*'
        // (ex. http://192.168.*:* pour joindre le LAN depuis un telephone malgre le DHCP),
        // compatible avec allowCredentials=true. Un match exact reste supporte.
        config.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
