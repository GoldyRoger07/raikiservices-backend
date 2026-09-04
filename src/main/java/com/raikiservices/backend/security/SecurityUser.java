package com.raikiservices.backend.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.raikiservices.backend.entity.Permission;
import com.raikiservices.backend.entity.Role;
import com.raikiservices.backend.entity.User;

/**
 * Adaptateur entre l'entité {@link User} et le contrat {@link UserDetails} de Spring Security.
 *
 * <p>Les rôles sont exposés avec le préfixe {@code ROLE_} (attendu par {@code hasRole(...)}),
 * les permissions sans préfixe (utilisables via {@code hasAuthority("blog:write")}).
 * Les noms de rôles sont donc stockés en base sans préfixe : {@code ADMIN}, pas {@code ROLE_ADMIN}.
 */
public class SecurityUser implements UserDetails {

    private final transient User user;
    private final Set<GrantedAuthority> authorities;

    public SecurityUser(User user) {
        this.user = user;
        this.authorities = buildAuthorities(user);
    }

    private static Set<GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> result = new LinkedHashSet<>();
        if (user.getRoles() == null) {
            return result;
        }
        for (Role role : user.getRoles()) {
            result.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            if (role.getPermissions() == null) {
                continue;
            }
            for (Permission permission : role.getPermissions()) {
                result.add(new SimpleGrantedAuthority(permission.getName()));
            }
        }
        return result;
    }

    public User getUser() {
        return user;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /** L'email sert d'identifiant technique : il est unique et stable côté frontend. */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
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
}
