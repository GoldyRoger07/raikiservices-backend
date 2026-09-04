package com.raikiservices.backend.dto.auth;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.raikiservices.backend.entity.Role;
import com.raikiservices.backend.entity.User;

/** Profil renvoyé au frontend après connexion : jamais le mot de passe. */
public record UserSummaryResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String position,
        String photoUrl,
        Set<String> roles,
        Set<String> permissions) {

    public static UserSummaryResponse from(User user) {
        Set<String> roles = new TreeSet<>();
        Set<String> permissions = new TreeSet<>();

        List<Role> userRoles = user.getRoles() == null ? List.of() : List.copyOf(user.getRoles());
        for (Role role : userRoles) {
            roles.add(role.getName());
            if (role.getPermissions() != null) {
                role.getPermissions().forEach(p -> permissions.add(p.getName()));
            }
        }

        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPosition(),
                user.getPhotoUrl(),
                roles,
                permissions);
    }
}
