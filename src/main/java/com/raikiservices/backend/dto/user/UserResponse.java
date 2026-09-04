package com.raikiservices.backend.dto.user;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.raikiservices.backend.entity.Role;
import com.raikiservices.backend.entity.User;
import com.raikiservices.backend.entity.UserStatus;

/**
 * Compte tel que le voit l'administration. Le mot de passe n'y figure sous aucune forme.
 *
 * @param permissions permissions effectives, cumulées sur tous les rôles du compte : le
 *                    back-office affiche ainsi les droits réels sans recalculer l'union
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        String position,
        String bio,
        String photoUrl,
        boolean enabled,
        UserStatus status,
        Instant createdAt,
        List<RoleRef> roles,
        Set<String> permissions) {

    /** Rôle réduit à ce dont le back-office a besoin : l'afficher et le resoumettre. */
    public record RoleRef(Long id, String name) {
    }

    public static UserResponse from(User user) {
        Set<Role> userRoles = user.getRoles();
        List<RoleRef> roles = userRoles == null ? List.of()
                : userRoles.stream()
                        .sorted(Comparator.comparing(Role::getName))
                        .map(r -> new RoleRef(r.getId(), r.getName()))
                        .toList();

        Set<String> permissions = new TreeSet<>();
        if (userRoles != null) {
            for (Role role : userRoles) {
                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(p -> permissions.add(p.getName()));
                }
            }
        }

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getPosition(),
                user.getBio(),
                user.getPhotoUrl(),
                user.isEnabled(),
                user.getStatus(),
                user.getCreatedAt(),
                roles,
                permissions);
    }
}
