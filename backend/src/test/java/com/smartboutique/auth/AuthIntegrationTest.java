package com.smartboutique.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartboutique.dto.LoginRequest;
import com.smartboutique.dto.ResetPasswordRequest;
import com.smartboutique.entity.PasswordResetToken;
import com.smartboutique.entity.User;
import com.smartboutique.repository.PasswordResetTokenRepository;
import com.smartboutique.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.smartboutique.support.AbstractPostgresIT;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'integration de l'authentification et des autorisations (Phase 2).
 */
@AutoConfigureMockMvc
class AuthIntegrationTest extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private static final String ADMIN_EMAIL = "admin@smartboutique.com";
    private static final String ADMIN_PASSWORD = "Admin@123";

    private static final String VENDEUR_EMAIL = "vendeur@smartboutique.com";
    private static final String VENDEUR_PASSWORD = "vendeur123";
    private static final String DISABLED_EMAIL = "inactif@smartboutique.com";

    @BeforeEach
    void setUp() {
        // Etat propre pour les comptes de test (l'admin est seede au demarrage).
        tokenRepository.deleteAll();
        userRepository.findByEmail(VENDEUR_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(DISABLED_EMAIL).ifPresent(userRepository::delete);

        userRepository.save(User.builder()
                .fullName("Vendeur Actif").email(VENDEUR_EMAIL)
                .password(passwordEncoder.encode(VENDEUR_PASSWORD))
                .active(true).build());

        userRepository.save(User.builder()
                .fullName("Vendeur Inactif").email(DISABLED_EMAIL)
                .password(passwordEncoder.encode(VENDEUR_PASSWORD))
                .active(false).build());
    }

    private String login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(email, password));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test
    @DisplayName("Login reussi : renvoie un token JWT et le role")
    void login_success() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.platformAdmin").value(false))
                .andExpect(jsonPath("$.user.email").value(ADMIN_EMAIL));
    }

    @Test
    @DisplayName("Login d'un compte desactive : refuse (403)")
    void login_disabledAccount_isForbidden() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(DISABLED_EMAIL, VENDEUR_PASSWORD));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("desactive")));
    }

    @Test
    @DisplayName("Login avec mauvais mot de passe : 401")
    void login_badPassword_isUnauthorized() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(VENDEUR_EMAIL, "mauvais"));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Route admin sans le role ADMIN (vendeur) : refuse (403)")
    void adminRoute_asVendeur_isForbidden() throws Exception {
        String token = login(VENDEUR_EMAIL, VENDEUR_PASSWORD);
        mockMvc.perform(get("/api/admin/sellers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @com.smartboutique.support.WithShopMember(role = "OWNER")
    @DisplayName("Route boutique avec l'autorite OWNER (X-Shop-Id valide) : autorise (200)")
    void shopRoute_asOwner_isOk() throws Exception {
        mockMvc.perform(get("/api/admin/sellers"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Route protegee sans token : 401")
    void protectedRoute_noToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/sellers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Reset password avec un token expire : refuse (400)")
    void resetPassword_expiredToken_isBadRequest() throws Exception {
        User vendeur = userRepository.findByEmail(VENDEUR_EMAIL).orElseThrow();
        PasswordResetToken expired = tokenRepository.save(PasswordResetToken.builder()
                .token("token-expire-123")
                .user(vendeur)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .used(false)
                .build());

        String body = objectMapper.writeValueAsString(
                new ResetPasswordRequest(expired.getToken(), "nouveauMotDePasse"));
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("expire")));
    }

    @Test
    @DisplayName("Reset password avec un token valide : met a jour le mot de passe (200)")
    void resetPassword_validToken_isOk() throws Exception {
        User vendeur = userRepository.findByEmail(VENDEUR_EMAIL).orElseThrow();
        tokenRepository.save(PasswordResetToken.builder()
                .token("token-valide-123")
                .user(vendeur)
                .expiresAt(LocalDateTime.now().plusMinutes(20))
                .used(false)
                .build());

        String newPassword = "motDePasseModifie";
        String body = objectMapper.writeValueAsString(
                new ResetPasswordRequest("token-valide-123", newPassword));
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        // Le nouveau mot de passe doit permettre la connexion.
        login(VENDEUR_EMAIL, newPassword);

        // Le token doit etre marque comme utilise.
        assertThat(tokenRepository.findByToken("token-valide-123").orElseThrow().isUsed()).isTrue();
    }
}
