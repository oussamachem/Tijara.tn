package com.smartboutique.security;

import com.smartboutique.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Generation et verification des access tokens JWT (HS256).
 *
 * <p>Phase A : le token porte l'IDENTITE seule (user_id en subject, email, is_platform_admin).
 * PLUS de role ni de boutique figee : le role est contextuel (shop_members) et la boutique active
 * est resolue par requete via le header X-Shop-Id.</p>
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** Genere un token d'IDENTITE : subject = user id, + email + flag plateforme. */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("platform_admin", user.isPlatformAdmin())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return parse(token).get("email", String.class);
    }

    public Long extractUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public boolean extractPlatformAdmin(String token) {
        return Boolean.TRUE.equals(parse(token).get("platform_admin", Boolean.class));
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
