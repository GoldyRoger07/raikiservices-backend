package com.raikiservices.backend.dto.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Création ou modification d'une permission.
 *
 * <p>Le nom suit la convention {@code ACTION_RESSOURCE} en majuscules : c'est la chaîne
 * littérale qu'attendent les {@code @PreAuthorize("hasAuthority(...)")} des contrôleurs, d'où
 * le format imposé.
 */
public record PermissionRequest(
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères.")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$",
                message = "Le nom doit être en majuscules, chiffres et tirets bas (ex. READ_BLOG).")
        String name,

        @Size(max = 50, message = "Le module ne peut pas dépasser 50 caractères.")
        String module,

        @Size(max = 50, message = "L'action ne peut pas dépasser 50 caractères.")
        String action,

        @Size(max = 255, message = "La description ne peut pas dépasser 255 caractères.")
        String description) {
}
