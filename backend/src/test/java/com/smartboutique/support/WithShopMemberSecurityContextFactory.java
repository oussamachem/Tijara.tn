package com.smartboutique.support;

import com.smartboutique.entity.User;
import com.smartboutique.repository.UserRepository;
import com.smartboutique.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Construit le contexte de securite pour {@link WithShopMember} (principal reel + autorites). */
@Component
public class WithShopMemberSecurityContextFactory implements WithSecurityContextFactory<WithShopMember> {

    @Autowired
    private UserRepository userRepository;

    @Override
    public SecurityContext createSecurityContext(WithShopMember annotation) {
        User user = userRepository.findByEmail(annotation.email())
                .orElseThrow(() -> new IllegalStateException(
                        "Compte de test introuvable : " + annotation.email()));

        UserPrincipal principal = new UserPrincipal(user);
        List<GrantedAuthority> authorities = new ArrayList<>(principal.getAuthorities());
        authorities.add(new SimpleGrantedAuthority("ROLE_SHOP_" + annotation.role()));
        if (annotation.platformAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
        return context;
    }
}
