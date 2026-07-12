package com.smartboutique.security;

import com.smartboutique.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Adaptateur User -> UserDetails. Phase A : le principal porte l'IDENTITE. Les seules autorites
 * GLOBALES sont ROLE_PLATFORM_ADMIN (si {@code isPlatformAdmin}). Les autorites de BOUTIQUE
 * (ROLE_SHOP_OWNER / ROLE_SHOP_VENDOR) sont ajoutees par requete par le filtre X-Shop-Id, apres
 * validation du membership.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final boolean active;
    private final boolean platformAdmin;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.active = user.isActive();
        this.platformAdmin = user.isPlatformAdmin();
        List<GrantedAuthority> auths = new ArrayList<>();
        if (user.isPlatformAdmin()) auths.add(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
        this.authorities = List.copyOf(auths);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
