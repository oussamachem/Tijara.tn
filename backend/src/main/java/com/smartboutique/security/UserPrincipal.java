package com.smartboutique.security;

import com.smartboutique.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptateur entre notre entite {@link User} et le contrat Spring Security {@link UserDetails}.
 * L'identite (username) est l'email ; l'autorite est "ROLE_<role>".
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final boolean active;
    private final Long boutiqueId;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this(user, true);
    }

    /**
     * @param boutiqueActive false si la boutique du user est SUSPENDED -> compte considere non actif
     *                       (connexion refusee, meme pour un user actif).
     */
    public UserPrincipal(User user, boolean boutiqueActive) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.active = user.isActive() && boutiqueActive;
        this.boutiqueId = user.getBoutiqueId();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
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

    /** Un compte desactive (active=false) est considere comme non active : connexion refusee. */
    @Override
    public boolean isEnabled() {
        return active;
    }
}
