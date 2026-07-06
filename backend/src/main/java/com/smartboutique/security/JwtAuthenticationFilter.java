package com.smartboutique.security;

import com.smartboutique.tenancy.TenantContext;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre execute une fois par requete : lit le header "Authorization: Bearer ...",
 * valide le JWT et place l'utilisateur authentifie dans le contexte de securite.
 * En l'absence de token (ou token invalide), la requete continue sans authentification
 * (les routes publiques restent accessibles, les autres renvoient 401 via l'entry point).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String email = jwtService.extractEmail(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Refuse un token valide dont le compte a ete desactive entre-temps.
                if (userDetails.isEnabled()) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Contexte tenant de la requete (base de la RLS). Source autoritaire = la boutique
                    // de l'utilisateur charge en base ; a defaut, le claim du token.
                    Long boutiqueId = (userDetails instanceof UserPrincipal p) ? p.getBoutiqueId() : null;
                    if (boutiqueId == null) boutiqueId = jwtService.extractBoutiqueId(token);
                    TenantContext.set(boutiqueId);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                // Token invalide ou expire : on n'authentifie pas, l'entry point renverra 401.
                log.debug("JWT invalide : {}", ex.getMessage());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Indispensable : le thread retourne au pool, ne pas laisser fuiter le tenant.
            TenantContext.clear();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());
        }
        return null;
    }
}
