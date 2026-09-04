package com.raikiservices.backend.dto.permission;

import java.time.Instant;

import com.raikiservices.backend.entity.Permission;

/**
 * @param system permission seedée au démarrage : ni renommable, ni supprimable, puisque son
 *               nom est référencé en dur dans les annotations de sécurité
 */
public record PermissionResponse(
        Long id,
        String name,
        String module,
        String action,
        String description,
        boolean system,
        Instant createdAt) {

    public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getName(),
                permission.getModule(),
                permission.getAction(),
                permission.getDescription(),
                permission.isSystem(),
                permission.getCreatedAt());
    }
}
