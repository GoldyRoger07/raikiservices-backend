package com.raikiservices.backend.dto.role;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param name          sans préfixe {@code ROLE_} : celui-ci est ajouté à la volée par
 *                      {@code SecurityUser} au moment de construire les autorisations
 * @param permissionIds jeu complet des permissions du rôle — la liste envoyée remplace
 *                      l'existante, elle ne s'y ajoute pas
 */
public record RoleRequest(
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 50, message = "Le nom ne peut pas dépasser 50 caractères.")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$",
                message = "Le nom doit être en majuscules, chiffres et tirets bas (ex. EDITOR).")
        String name,

        @Size(max = 255, message = "La description ne peut pas dépasser 255 caractères.")
        String description,

        Set<Long> permissionIds) {
}
