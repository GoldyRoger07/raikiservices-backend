package com.raikiservices.backend.dto.media;

/**
 * Autorisation d'envoi direct vers Cloudinary.
 *
 * <p>Le navigateur poste au {@code uploadUrl} un formulaire multipart contenant le fichier,
 * {@code api_key}, {@code timestamp}, {@code folder} et {@code signature} — exactement ces
 * champs : Cloudinary recalcule l'empreinte sur ce qu'il reçoit et refuse au moindre écart.
 *
 * <p>Ni le secret d'API ni aucune capacité d'écriture durable ne sortent d'ici : la
 * signature ne vaut que pour cet envoi, et Cloudinary rejette un horodatage périmé.
 *
 * @param maxBytes taille acceptée, à faire respecter côté navigateur avant l'envoi
 */
public record UploadSignatureResponse(
        String cloudName,
        String apiKey,
        long timestamp,
        String folder,
        String signature,
        String uploadUrl,
        long maxBytes) {
}
