package com.raikiservices.backend.dto.media;

/**
 * Autorisation d'envoi direct vers ImageKit.
 *
 * <p>Le navigateur poste au {@code uploadUrl} un formulaire multipart contenant le fichier,
 * {@code fileName}, puis {@code publicKey}, {@code token}, {@code expire}, {@code signature}
 * et {@code folder} repris tels quels : ImageKit recalcule l'empreinte sur le couple
 * {@code token + expire} reçu et refuse au moindre écart.
 *
 * <p>Ni la clé privée ni aucune capacité d'écriture durable ne sortent d'ici : le triplet
 * ne vaut qu'une minute, et ImageKit rejette un jeton déjà consommé.
 *
 * @param urlEndpoint base des adresses de livraison, pour recomposer les URL côté client
 * @param expire      horodatage Unix, en secondes, au-delà duquel l'autorisation est caduque
 * @param folder      dossier de destination, à envoyer tel quel avec le fichier
 * @param maxBytes    taille acceptée, à faire respecter côté navigateur avant l'envoi
 */
public record UploadSignatureResponse(
        String publicKey,
        String urlEndpoint,
        String token,
        long expire,
        String signature,
        String folder,
        String uploadUrl,
        long maxBytes) {
}
