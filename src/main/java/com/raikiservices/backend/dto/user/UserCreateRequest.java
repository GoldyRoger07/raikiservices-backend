package com.raikiservices.backend.dto.user;

import java.util.Set;

import com.raikiservices.backend.entity.UserStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Création d'un compte depuis le back-office.
 *
 * <p>Contrairement à l'inscription publique, le compte est actif immédiatement : c'est un
 * administrateur qui le crée, l'adresse n'a donc pas à être prouvée par OTP.
 *
 * @param enabled {@code true} par défaut
 * @param status  {@code ACTIVE} par défaut
 * @param roleIds rôles attribués ; un compte sans rôle peut se connecter mais n'a aucun droit
 */
public record UserCreateRequest(
        @NotBlank(message = "Le nom d'utilisateur est obligatoire.")
        @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit faire entre 3 et 50 caractères.")
        String username,

        @NotBlank(message = "L'adresse email est obligatoire.")
        @Email(message = "L'adresse email n'est pas valide.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
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
