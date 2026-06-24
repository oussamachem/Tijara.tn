package com.smartboutique.dto;

/** Reponse de connexion : token JWT + infos utilisateur. */
public record AuthResponse(
        String token,
        String tokenType,
        UserResponse user
) {
    public static AuthResponse of(String token, UserResponse user) {
        return new AuthResponse(token, "Bearer", user);
    }
}
