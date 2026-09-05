package com.smartboutique.security;

import com.smartboutique.entity.BoutiqueStatus;
import com.smartboutique.entity.ShopMember;
import com.smartboutique.repository.BoutiqueRepository;
import com.smartboutique.repository.ShopMemberRepository;
import com.smartboutique.tenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Filtre par requête : (1) authentifie l'IDENTITÉ via un JWT <b>Keycloak</b> (OIDC) ; (2) résout la
 * BOUTIQUE ACTIVE via {@code X-Shop-Id}, VALIDÉE contre {@code shop_members}, et pose le tenant (RLS).
 *
 * <p>Le token Keycloak est validé (signature JWKS + issuer + expiration). L'email du token identifie
 * l'utilisateur applicatif (créé à la volée si absent — JIT) : son {@code id} est celui référencé
 * partout (RLS, commandes, favoris). Le rôle realm {@code admin} -> {@code ROLE_PLATFORM_ADMIN}.
 * Les rôles de boutique {@code ROLE_SHOP_OWNER/VENDOR} restent posés par {@code shop_members} +
 * {@code X-Shop-Id} -> la RLS n'est pas touchée.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";
    private static final String SHOP_HEADER = "X-Shop-Id";

    private final JwtDecoder jwtDecoder;
    private final CustomUserDetailsService userDetailsService;
    private final ShopMemberRepository shopMemberRepository;
    private final BoutiqueRepository boutiqueRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Jwt jwt = jwtDecoder.decode(token);   // signature JWKS + issuer + expiration
                String email = jwt.getClaimAsString("email");
                if (!StringUtils.hasText(email)) email = jwt.getClaimAsString("preferred_username");

                if (StringUtils.hasText(email)) {
                    boolean isAdmin = realmRoles(jwt).contains("admin");
                    UserPrincipal principal = userDetailsService.loadOrCreateByEmail(
                            email, jwt.getClaimAsString("name"), isAdmin);

                    if (principal.isEnabled()) {
                        List<GrantedAuthority> authorities = new ArrayList<>(principal.getAuthorities());
                        if (isAdmin) authorities.add(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
                        // Boutique active resolue par X-Shop-Id (ajoute un role de boutique + tenant/RLS).
                        resolveActiveShop(request, principal, authorities);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        principal, null, authorities.stream().distinct().toList());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("Token Keycloak invalide : {}", ex.getMessage());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /** Rôles realm Keycloak ({@code realm_access.roles}). */
    @SuppressWarnings("unchecked")
    private List<String> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            return roles.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    /**
     * Valide {@code X-Shop-Id} contre le membership de l'utilisateur. Si OK et boutique active :
     * ajoute le rôle de boutique et pose le tenant. Sinon : rien (accès boutique -> 403).
     */
    private void resolveActiveShop(HttpServletRequest request, UserPrincipal principal,
                                   List<GrantedAuthority> authorities) {
        String header = request.getHeader(SHOP_HEADER);
        if (!StringUtils.hasText(header)) return;

        Long shopId;
        try {
            shopId = Long.valueOf(header.trim());
        } catch (NumberFormatException e) {
            return;
        }

        ShopMember membership = shopMemberRepository
                .findByShopIdAndUserId(shopId, principal.getId()).orElse(null);
        if (membership == null) return;

        boolean suspended = boutiqueRepository.findById(shopId)
                .map(b -> b.getStatus() == BoutiqueStatus.SUSPENDED).orElse(true);
        if (suspended) return;

        authorities.add(new SimpleGrantedAuthority("ROLE_SHOP_" + membership.getRole().name())); // OWNER|VENDOR
        TenantContext.set(shopId);        // -> SET LOCAL app.current_boutique (RLS)
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());
        }
        return null;
    }
}
