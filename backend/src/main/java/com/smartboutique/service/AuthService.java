package com.smartboutique.service;

import com.smartboutique.dto.*;
import com.smartboutique.entity.PasswordResetToken;
import com.smartboutique.entity.User;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.mapper.UserMapper;
import com.smartboutique.repository.PasswordResetTokenRepository;
import com.smartboutique.repository.UserRepository;
import com.smartboutique.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Authentification : connexion (JWT), mot de passe oublie et reinitialisation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Value("${app.password-reset.expiration-ms}")
    private long resetExpirationMs;

    /**
     * Authentifie l'utilisateur et renvoie un JWT.
     * Les comptes desactives sont rejetes (DisabledException -> 403, gere par l'advice).
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Verifie email + mot de passe (et le statut actif via UserDetails.isEnabled()).
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("Identifiants invalides", HttpStatus.UNAUTHORIZED));

        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, userMapper.toResponse(user));
    }

    /**
     * Genere un token de reinitialisation (avec expiration) et simule l'envoi d'email (log).
     * Renvoie toujours un message generique pour ne pas reveler l'existence d'un compte.
     */
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            // Invalide les eventuels tokens precedents puis en cree un nouveau.
            tokenRepository.markAllUsedForUser(user);

            PasswordResetToken token = PasswordResetToken.builder()
                    .token(UUID.randomUUID().toString())
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusNanos(resetExpirationMs * 1_000_000))
                    .used(false)
                    .build();
            tokenRepository.save(token);

            // Simulation d'envoi d'email en developpement.
            log.info("=================================================================");
            log.info(" [SIMULATION EMAIL] Reinitialisation de mot de passe pour {}", user.getEmail());
            log.info("   Token : {}", token.getToken());
            log.info("   Expire le : {}", token.getExpiresAt());
            log.info("=================================================================");
        });

        return new MessageResponse(
                "Si un compte existe pour cet email, un lien de reinitialisation a ete envoye.");
    }

    /** Valide le token (existence, non utilise, non expire) puis met a jour le mot de passe. */
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> new BusinessException("Token de reinitialisation invalide", HttpStatus.BAD_REQUEST));

        if (token.isUsed()) {
            throw new BusinessException("Ce token a deja ete utilise", HttpStatus.BAD_REQUEST);
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Ce token a expire", HttpStatus.BAD_REQUEST);
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        log.info("Mot de passe reinitialise pour l'utilisateur {}", user.getEmail());
        return new MessageResponse("Votre mot de passe a ete reinitialise avec succes.");
    }
}
