package com.smartboutique.security;

import com.smartboutique.entity.BoutiqueStatus;
import com.smartboutique.entity.ShopMember;
import com.smartboutique.repository.BoutiqueRepository;
import com.smartboutique.repository.ShopMemberRepository;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtre par requete : (1) authentifie l'IDENTITE via le JWT ; (2) resout la BOUTIQUE ACTIVE via le
 * header {@code X-Shop-Id}, VALIDE contre {@code shop_members}, et pose le tenant courant.
 *
 * <p>Si X-Shop-Id designe une boutique dont l'utilisateur est membre (et active), on ajoute
 * l'autorite ROLE_SHOP_OWNER / ROLE_SHOP_VENDOR (selon le membership) et on pose
 * {@link TenantContext} -> l'aspect fera {@code SET LOCAL app.current_boutique} (RLS). Sinon, aucune
 * autorite de boutique n'est accordee -> les routes "espace boutique" renvoient 403.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";
    private static final String SHOP_HEADER = "X-Shop-Id";

    private final JwtService jwtService;
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
                String email = jwtService.extractEmail(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (userDetails.isEnabled() && userDetails instanceof UserPrincipal principal) {
                    // Autorites de base (identite) : ROLE_PLATFORM_ADMIN eventuel.
                    List<GrantedAuthority> authorities = new ArrayList<>(principal.getAuthorities());
                    // Boutique active resolue par X-Shop-Id (ajoute une autorite de boutique + tenant).
                    resolveActiveShop(request, principal, authorities);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("JWT invalide : {}", ex.getMessage());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Valide le header X-Shop-Id contre le membership de l'utilisateur. Si OK et boutique active :
     * ajoute l'autorite de boutique et pose le tenant. Sinon : ne fait rien (acces boutique -> 403).
     */
    private void resolveActiveShop(HttpServletRequest request, UserPrincipal principal,
                                   List<GrantedAuthority> authorities) {
        String header = request.getHeader(SHOP_HEADER);
        if (!StringUtils.hasText(header)) return;

        Long shopId;
        try {
            shopId = Long.valueOf(header.trim());
        } catch (NumberFormatException e) {
            return;   // header invalide -> pas d'acces boutique
        }

        ShopMember membership = shopMemberRepository
                .findByShopIdAndUserId(shopId, principal.getId()).orElse(null);
        if (membership == null) return;   // pas membre -> pas d'acces (403 en aval)

        boolean suspended = boutiqueRepository.findById(shopId)
                .map(b -> b.getStatus() == BoutiqueStatus.SUSPENDED).orElse(true);
        if (suspended) return;            // boutique suspendue -> pas d'acces

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
