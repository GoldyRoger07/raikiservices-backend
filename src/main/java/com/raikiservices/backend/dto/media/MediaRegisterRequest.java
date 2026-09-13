package com.raikiservices.backend.dto.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Déclaration d'une image tout juste téléversée, reprise de la réponse d'ImageKit.
 *
 * <p>Deux identifiants, deux usages : {@code fileId} est la clé qu'ImageKit exige pour
 * supprimer, {@code publicId} est le chemin ({@code filePath}) auquel les projets se
 * réfèrent et à partir duquel le site recompose ses adresses.
 *
 * <p>Les dimensions et le poids sont rapportés par le navigateur : ce sont des indications
 * d'affichage pour la bibliothèque, pas des données de sécurité. Seul un administrateur
 * authentifié peut les envoyer, et les fausser n'aurait d'autre effet que de mal ranger sa
 * propre bibliothèque.
 */
public record MediaRegisterRequest(
        @NotBlank(message = "L'identifiant ImageKit du fichier est obligatoire.")
        @Size(max = 255, message = "L'identifiant ne peut pas dépasser 255 caractères.")
        String fileId,

        @NotBlank(message = "Le chemin ImageKit est obligatoire.")
        @Size(max = 255, message = "Le chemin ne peut pas dépasser 255 caractères.")
        String publicId,

        @NotBlank(message = "L'adresse de l'image est obligatoire.")
        @Size(max = 1000, message = "L'adresse ne peut pas dépasser 1000 caractères.")
        String secureUrl,

        String format,
        Integer width,
        Integer height,
        Long bytes,
        String folder,
        String originalFilename,

        @Size(max = 500, message = "Le texte alternatif ne peut pas dépasser 500 caractères.")
        String alt) {
}
