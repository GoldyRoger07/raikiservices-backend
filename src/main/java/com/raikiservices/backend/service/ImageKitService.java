package com.raikiservices.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.raikiservices.backend.dto.media.UploadSignatureResponse;
import com.raikiservices.backend.exception.BusinessRuleException;

/**
 * Dialogue avec ImageKit : autorisation d'envoi et suppression.
 *
 * <p>Le fichier lui-même ne transite jamais par le backend. Celui-ci délivre une
 * autorisation d'envoi, le navigateur téléverse en direct vers ImageKit, puis vient
 * déclarer le résultat. Faire passer une image de plusieurs mégaoctets par ici la ferait
 * payer deux fois en bande passante d'hébergement — à l'entrée puis à la sortie vers
 * ImageKit — et immobiliserait un fil d'exécution le temps du transfert.
 *
 * <p>L'autorisation est un triplet {@code token} / {@code expire} / {@code signature}, où la
 * signature est l'empreinte HMAC-SHA1 de {@code token + expire} scellée par la clé privée.
 * Seule la clé <em>publique</em> descend jusqu'au navigateur ; la clé privée ne quitte
 * jamais le serveur.
 *
 * <p><strong>Portée de l'autorisation.</strong> Contrairement à Cloudinary, ImageKit ne
 * signe pas le dossier de destination : le triplet vaut pour n'importe quel envoi dans le
 * compte, pas seulement sous le dossier demandé. Deux garde-fous compensent — il n'est
 * délivré qu'à un administrateur porteur de {@code CREATE_MEDIA}, et il expire au bout
 * d'une minute. Le dossier reste donc une convention de rangement, pas une frontière de
 * sécurité.
 *
 * <p>Aucune dépendance ajoutée : {@link Mac} calcule le HMAC sans rien installer, et la
 * suppression tient en un appel HTTP. Le SDK officiel tirerait un client HTTP entier pour
 * ces deux seules opérations.
 */
@Service
public class ImageKitService {

    private static final Logger log = LoggerFactory.getLogger(ImageKitService.class);

    /** Point d'entrée des envois — distinct de l'API d'administration. */
    private static final String UPLOAD_URL = "https://upload.imagekit.io/api/v1/files/upload";

    /** API d'administration : suppression, listing, métadonnées. */
    private static final String API_BASE = "https://api.imagekit.io/v1";

    /**
     * Durée de validité de l'autorisation. ImageKit plafonne à une heure ; une minute suffit
     * à démarrer un envoi et réduit d'autant la fenêtre d'un triplet intercepté.
     */
    private static final long VALIDITY_SECONDS = 60;

    /** Sous-dossier accepté : minuscules, chiffres, tiret, tiret bas et séparateurs. */
    private static final Pattern SUBFOLDER = Pattern.compile("[a-z0-9][a-z0-9_/-]{0,79}");

    private final RestClient restClient = RestClient.create();

    private final String publicKey;
    private final String privateKey;
    private final String urlEndpoint;
    private final String rootFolder;
    private final long maxBytes;

    public ImageKitService(
            @Value("${imagekit.public-key:}") String publicKey,
            @Value("${imagekit.private-key:}") String privateKey,
            @Value("${imagekit.url-endpoint:}") String urlEndpoint,
            @Value("${imagekit.folder:raiki}") String rootFolder,
            @Value("${imagekit.max-bytes:5242880}") long maxBytes) {

        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.urlEndpoint = trimTrailingSlashes(urlEndpoint);
        this.rootFolder = "/" + trimSlashes(rootFolder);
        this.maxBytes = maxBytes;

        if (!isConfigured()) {
            log.warn("ImageKit non configuré (imagekit.public-key / private-key / url-endpoint) : "
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

        // Le jeton doit être unique : ImageKit refuse un jeton déjà vu, ce qui empêche de
        // rejouer une autorisation interceptée tant qu'elle n'a pas expiré.
        String token = UUID.randomUUID().toString();
        long expire = Instant.now().getEpochSecond() + VALIDITY_SECONDS;

        return new UploadSignatureResponse(
                publicKey,
                urlEndpoint,
                token,
                expire,
                sign(token + expire),
                folder,
                UPLOAD_URL,
                maxBytes);
    }

    /**
     * Supprime définitivement un fichier chez ImageKit.
     *
     * <p>La suppression se fait par {@code fileId}, l'identifiant rendu à l'envoi, et non par
     * chemin : c'est la seule clé qu'ImageKit accepte ici, et elle reste valable même si le
     * fichier est renommé ou déplacé depuis la console.
     *
     * <p>Un fichier déjà absent n'est pas une erreur : le but est qu'il ne soit plus là. Le
     * cache du CDN n'est volontairement pas purgé, opération lente et facturée ; les adresses
     * livrées portent le nom unique du fichier, qui ne sera pas réutilisé.
     *
     * @return {@code true} si ImageKit a bien supprimé le fichier
     */
    public boolean destroy(String fileId) {
        requireConfigured();

        try {
            ResponseEntity<Void> response = restClient.delete()
                    .uri(API_BASE + "/files/{fileId}", fileId)
                    .header(HttpHeaders.AUTHORIZATION, basicAuth())
                    .retrieve()
                    // 404 = déjà supprimé côté ImageKit : constat, pas incident.
                    .onStatus(status -> status.value() == 404, (request, ignored) -> { })
                    .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                return true;
            }
            log.warn("Suppression ImageKit de {} : fichier introuvable ({})",
                    fileId, response.getStatusCode());
            return false;

        } catch (RestClientException failure) {
            log.error("ImageKit injoignable pour la suppression de {}", fileId, failure);
            throw new BusinessRuleException(
                    "ImageKit n'a pas pu être joint : l'image n'a pas été supprimée.");
        }
    }

    public boolean isConfigured() {
        return !publicKey.isBlank() && !privateKey.isBlank() && !urlEndpoint.isBlank();
    }

    public String rootFolder() {
        return rootFolder;
    }

    /** Base des adresses de livraison — « https://ik.imagekit.io/moncompte ». */
    public String urlEndpoint() {
        return urlEndpoint;
    }

    // ──────────────── Interne ────────────────

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new BusinessRuleException(
                    "Le service d'images n'est pas configuré. Renseignez les identifiants ImageKit.");
        }
    }

    /**
     * Range le sous-dossier demandé sous le dossier racine.
     *
     * <p>Le sous-dossier arrive du client : sans ce filtre, il nommerait n'importe quel
     * emplacement du compte ImageKit, hors de portée de la bibliothèque du back-office.
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
     * L'API d'administration s'authentifie en Basic HTTP : la clé privée tient lieu
     * d'identifiant, le mot de passe reste vide.
     */
    private String basicAuth() {
        String credentials = privateKey + ":";
        return "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /** Empreinte attendue par ImageKit : HMAC-SHA1 hexadécimal, scellé par la clé privée. */
    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException impossible) {
            // HmacSHA1 fait partie des algorithmes que toute JVM doit fournir.
            throw new IllegalStateException("HmacSHA1 indisponible", impossible);
        } catch (InvalidKeyException badKey) {
            throw new IllegalStateException("Clé privée ImageKit inutilisable", badKey);
        }
    }

    private static String trimSlashes(String value) {
        return value == null ? "" : value.replaceAll("^/+|/+$", "");
    }

    private static String trimTrailingSlashes(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }
}
