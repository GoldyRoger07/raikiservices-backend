package com.raikiservices.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.raikiservices.backend.dto.media.UploadSignatureResponse;
import com.raikiservices.backend.exception.BusinessRuleException;

/**
 * Dialogue avec Cloudinary : signature d'envoi et suppression.
 *
 * <p>Le fichier lui-même ne transite jamais par le backend. Celui-ci signe une autorisation
 * d'envoi, le navigateur téléverse en direct, puis vient déclarer le résultat. Faire passer
 * une image de plusieurs mégaoctets par ici la ferait payer deux fois en bande passante
 * d'hébergement — à l'entrée puis à la sortie vers Cloudinary — et immobiliserait un fil
 * d'exécution le temps du transfert.
 *
 * <p>Envoi <em>signé</em> et non « unsigned preset » : un preset non signé est un droit
 * d'écriture public, lisible dans le bundle JavaScript, avec lequel n'importe qui remplirait
 * le compte. La signature, elle, n'est délivrée qu'à un administrateur authentifié et
 * n'est valable qu'une minute.
 *
 * <p>Aucune dépendance ajoutée : la signature est une empreinte SHA-1 des paramètres triés,
 * que {@link MessageDigest} calcule sans rien installer. Le SDK officiel tirerait un client
 * HTTP entier pour cette seule opération.
 */
@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    private static final String API_BASE = "https://api.cloudinary.com/v1_1/";

    /** Sous-dossier accepté : minuscules, chiffres, tiret, tiret bas et séparateurs. */
    private static final Pattern SUBFOLDER = Pattern.compile("[a-z0-9][a-z0-9_/-]{0,79}");

    private final RestClient restClient = RestClient.create();

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String rootFolder;
    private final long maxBytes;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret,
            @Value("${cloudinary.folder:raiki}") String rootFolder,
            @Value("${cloudinary.max-bytes:5242880}") long maxBytes) {

        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.rootFolder = trimSlashes(rootFolder);
        this.maxBytes = maxBytes;

        if (!isConfigured()) {
            log.warn("Cloudinary non configuré (cloudinary.cloud-name / api-key / api-secret) : "
                    + "l'envoi d'images restera indisponible.");
        }
    }

    /**
     * Autorisation d'envoi à usage unique.
     *
     * @param subfolder sous-dossier facultatif, rangé sous le dossier racine configuré
     */
    public UploadSignatureResponse signUpload(String subfolder) {
        requireConfigured();

        String folder = resolveFolder(subfolder);
        long timestamp = Instant.now().getEpochSecond();

        // Paramètres effectivement signés : ce sont exactement ceux que le navigateur doit
        // envoyer en plus du fichier et de la clé publique, ni plus ni moins — Cloudinary
        // recalcule l'empreinte sur ce qu'il reçoit et refuse au moindre écart.
        Map<String, String> signed = new LinkedHashMap<>();
        signed.put("folder", folder);
        signed.put("timestamp", String.valueOf(timestamp));

        return new UploadSignatureResponse(
                cloudName,
                apiKey,
                timestamp,
                folder,
                sign(signed),
                API_BASE + cloudName + "/image/upload",
                maxBytes);
    }

    /**
     * Supprime définitivement une image chez Cloudinary.
     *
     * <p>Une image déjà absente n'est pas une erreur : le but est qu'elle ne soit plus là.
     * Le cache du CDN n'est volontairement pas purgé ({@code invalidate}), opération lente
     * et facturée ; les adresses livrées portent l'identifiant du fichier, qui ne sera pas
     * réutilisé.
     *
     * @return {@code true} si Cloudinary a bien supprimé le fichier
     */
    public boolean destroy(String publicId) {
        requireConfigured();

        long timestamp = Instant.now().getEpochSecond();
        Map<String, String> signed = new LinkedHashMap<>();
        signed.put("public_id", publicId);
        signed.put("timestamp", String.valueOf(timestamp));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("public_id", publicId);
        form.add("timestamp", String.valueOf(timestamp));
        form.add("api_key", apiKey);
        form.add("signature", sign(signed));

        try {
            Map<?, ?> response = restClient.post()
                    .uri(API_BASE + cloudName + "/image/destroy")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            Object result = response == null ? null : response.get("result");
            if ("ok".equals(result)) {
                return true;
            }
            log.warn("Suppression Cloudinary de {} : réponse « {} »", publicId, result);
            return false;

        } catch (RestClientException failure) {
            log.error("Cloudinary injoignable pour la suppression de {}", publicId, failure);
            throw new BusinessRuleException(
                    "Cloudinary n'a pas pu être joint : l'image n'a pas été supprimée.");
        }
    }

    public boolean isConfigured() {
        return !cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
    }

    public String rootFolder() {
        return rootFolder;
    }

    // ──────────────── Interne ────────────────

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new BusinessRuleException(
                    "Le service d'images n'est pas configuré. Renseignez les identifiants Cloudinary.");
        }
    }

    /**
     * Range le sous-dossier demandé sous le dossier racine.
     *
     * <p>Le sous-dossier arrive du client : sans ce filtre, il pourrait déposer n'importe où
     * dans le compte Cloudinary, hors de portée de la bibliothèque du back-office.
     */
    private String resolveFolder(String subfolder) {
        if (subfolder == null || subfolder.isBlank()) {
            return rootFolder;
        }
        String cleaned = trimSlashes(subfolder.trim().toLowerCase());
        if (!SUBFOLDER.matcher(cleaned).matches() || cleaned.contains("..")) {
            throw new BusinessRuleException("Nom de dossier invalide : " + subfolder);
        }
        return rootFolder + "/" + cleaned;
    }

    /**
     * Empreinte attendue par Cloudinary : les paramètres triés par nom, joints en
     * {@code clé=valeur&…}, suffixés du secret, le tout en SHA-1 hexadécimal.
     */
    private String sign(Map<String, String> params) {
        String payload = params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        return sha1Hex(payload + apiSecret);
    }

    private static String sha1Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1")
                    .digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException impossible) {
            // SHA-1 fait partie des algorithmes que toute JVM doit fournir.
            throw new IllegalStateException("SHA-1 indisponible", impossible);
        }
    }

    private static String trimSlashes(String value) {
        return value == null ? "" : value.replaceAll("^/+|/+$", "");
    }
}
