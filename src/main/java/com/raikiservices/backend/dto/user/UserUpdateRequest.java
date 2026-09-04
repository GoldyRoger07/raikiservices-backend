package com.raikiservices.backend.dto.user;

import java.util.Set;

import com.raikiservices.backend.entity.UserStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Modification d'un compte depuis le back-office.
 *
 * @param password laisser {@code null} pour conserver le mot de passe actuel ; le renseigner
 *                 le remplace et ferme toutes les sessions ouvertes du compte
 * @param roleIds  jeu complet des rôles — la liste envoyée remplace l'existante
 */
public record UserUpdateRequest(
        @NotBlank(message = "Le nom d'utilisateur est obligatoire.")
        @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit faire entre 3 et 50 caractères.")
        String username,

        @NotBlank(message = "L'adresse email est obligatoire.")
        @Email(message = "L'adresse email n'est pas valide.")
        String email,

        @Size(min = 8, max = 100, message = "Le mot de passe doit faire au moins 8 caractères.")
        String password,

        String firstName,
        String lastName,
        String phone,
        String position,

        @Size(max = 500, message = "La biographie ne peut pas dépasser 500 caractères.")
        String bio,

        String photoUrl,
        Boolean enabled,
        UserStatus status,
        Set<Long> roleIds) {
}
