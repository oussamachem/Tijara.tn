package com.smartboutique.service;

import com.smartboutique.dto.AuthResponse;
import com.smartboutique.dto.ClientRegisterRequest;
import com.smartboutique.entity.User;
import com.smartboutique.exception.DuplicateResourceException;
import com.smartboutique.mapper.UserMapper;
import com.smartboutique.repository.UserRepository;
import com.smartboutique.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Inscription des comptes CLIENT (marketplace) : compte GLOBAL, sans boutique fixe. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(ClientRegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Cet email est deja utilise");
        }
        User client = userRepository.save(User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .active(true)
                .platformAdmin(false)   // compte global : aucun role fixe, aucune boutique
                .build());
        log.info("Compte CLIENT cree : {}", email);
        return AuthResponse.of(jwtService.generateToken(client), userMapper.toResponse(client));
    }
}
