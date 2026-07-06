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
 * Le secret et la duree d'expiration sont externalises (variables d'environnement / application.yml).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // La cle HMAC doit faire au moins 256 bits (32 octets) ; le secret par defaut respecte cette contrainte.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** Genere un token contenant l'id (subject), l'email, le role et la boutique (tenant). */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("boutique_id", user.getBoutiqueId())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** Tenant (boutique) porte par le token, ou null (ex. SUPER_ADMIN plateforme). */
    public Long extractBoutiqueId(String token) {
        Number b = parse(token).get("boutique_id", Number.class);
        return b != null ? b.longValue() : null;
    }

    /** Extrait l'email (claim) du token. Leve une exception JWT si le token est invalide ou expire. */
    public String extractEmail(String token) {
        return parse(token).get("email", String.class);
    }

    public Long extractUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
