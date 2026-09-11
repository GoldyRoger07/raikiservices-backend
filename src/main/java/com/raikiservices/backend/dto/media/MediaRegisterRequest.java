package com.raikiservices.backend.dto.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Déclaration d'une image tout juste téléversée, reprise de la réponse de Cloudinary.
 *
 * <p>Les dimensions et le poids sont rapportés par le navigateur : ce sont des indications
 * d'affichage pour la bibliothèque, pas des données de sécurité. Seul un administrateur
 * authentifié peut les envoyer, et les fausser n'aurait d'autre effet que de mal ranger sa
 * propre bibliothèque.
 */
public record MediaRegisterRequest(
        @NotBlank(message = "L'identifiant Cloudinary est obligatoire.")
        @Size(max = 255, message = "L'identifiant ne peut pas dépasser 255 caractères.")
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
