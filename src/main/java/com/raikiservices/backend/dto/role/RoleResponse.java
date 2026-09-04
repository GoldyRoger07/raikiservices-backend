package com.raikiservices.backend.dto.role;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.raikiservices.backend.entity.Permission;
import com.raikiservices.backend.entity.Role;

/**
 * @param system      rôle seedé au démarrage : non renommable, non supprimable, et dont les
 *                    permissions sont réalignées sur le code à chaque redémarrage
 * @param permissions triées par nom, pour que l'ordre d'affichage ne dépende pas du jeu
 */
public record RoleResponse(
        Long id,
        String name,
        String description,
        boolean system,
        Instant createdAt,
        List<PermissionRef> permissions) {

    /** Permission réduite à ce dont le back-office a besoin : l'afficher et la resoumettre. */
    public record PermissionRef(Long id, String name) {
    }

    public static RoleResponse from(Role role) {
        Set<Permission> rolePermissions = role.getPermissions();
        List<PermissionRef> permissions = rolePermissions == null ? List.of()
                : rolePermissions.stream()
                        .sorted(Comparator.comparing(Permission::getName))
                        .map(p -> new PermissionRef(p.getId(), p.getName()))
                        .toList();

        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.isSystem(),
                role.getCreatedAt(),
                permissions);
    }
}
