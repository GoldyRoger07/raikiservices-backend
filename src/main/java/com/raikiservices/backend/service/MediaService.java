package com.raikiservices.backend.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raikiservices.backend.dto.common.PageQuery;
import com.raikiservices.backend.dto.common.PageResponse;
import com.raikiservices.backend.dto.media.MediaAssetResponse;
import com.raikiservices.backend.dto.media.MediaRegisterRequest;
import com.raikiservices.backend.dto.media.MediaUpdateRequest;
import com.raikiservices.backend.dto.media.UploadSignatureResponse;
import com.raikiservices.backend.entity.MediaAsset;
import com.raikiservices.backend.exception.BusinessRuleException;
import com.raikiservices.backend.exception.ResourceNotFoundException;
import com.raikiservices.backend.repository.MediaAssetRepository;
import com.raikiservices.backend.repository.ProjectRepository;
import com.raikiservices.backend.repository.UserRepository;

/**
 * Bibliothèque d'images du back-office.
 *
 * <p>Tient l'inventaire des fichiers déposés chez Cloudinary et encadre leur cycle de vie :
 * autorisation d'envoi, déclaration après téléversement, suppression conjointe ici et
 * là-bas.
 */
@Service
public class MediaService {

    /** Champs sur lesquels le client peut trier — le paramètre finit dans un ORDER BY. */
    private static final Set<String> SORTABLE = Set.of(
            "publicId", "originalFilename", "format", "bytes", "folder", "uploadedAt");

    private final MediaAssetRepository mediaAssetRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinary;

    public MediaService(MediaAssetRepository mediaAssetRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            CloudinaryService cloudinary) {

        this.mediaAssetRepository = mediaAssetRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.cloudinary = cloudinary;
    }

    /** Autorisation d'envoi direct, à usage unique. */
    public UploadSignatureResponse signUpload(String subfolder) {
        return cloudinary.signUpload(subfolder);
    }

    /**
     * Enregistre une image tout juste téléversée.
     *
     * <p>Un identifiant déjà connu met la fiche à jour au lieu d'échouer : un envoi rejoué —
     * connexion coupée entre le téléversement et cet appel, bouton actionné deux fois — ne
     * doit pas laisser la bibliothèque en erreur alors que le fichier, lui, est bien arrivé.
     */
    @Transactional
    public MediaAssetResponse register(MediaRegisterRequest request, Long uploaderId) {
        MediaAsset asset = mediaAssetRepository.findByPublicId(request.publicId())
                .orElseGet(MediaAsset::new);

        asset.setPublicId(request.publicId());
        asset.setSecureUrl(request.secureUrl());
        asset.setFormat(request.format());
        asset.setWidth(request.width());
        asset.setHeight(request.height());
        asset.setBytes(request.bytes());
        asset.setFolder(request.folder() != null ? request.folder() : folderOf(request.publicId()));
        asset.setOriginalFilename(request.originalFilename());
        asset.setAlt(request.alt());

        if (asset.getUploadedBy() == null && uploaderId != null) {
            userRepository.findById(uploaderId).ifPresent(asset::setUploadedBy);
        }

        return MediaAssetResponse.from(mediaAssetRepository.save(asset));
    }

    @Transactional(readOnly = true)
    public PageResponse<MediaAssetResponse> getAll(PageQuery query, String folder) {
        Pageable pageable = query.toPageable(SORTABLE, "uploadedAt");
        String filter = query.filterOrNull();
        boolean scoped = folder != null && !folder.isBlank();

        Page<MediaAsset> result;
        if (filter == null) {
            result = scoped
                    ? mediaAssetRepository.findByFolderStartingWith(folder, pageable)
                    : mediaAssetRepository.findAll(pageable);
        } else {
            result = scoped
                    ? mediaAssetRepository.searchInFolder(filter, folder, pageable)
                    : mediaAssetRepository.search(filter, pageable);
        }

        return PageResponse.from(result, MediaAssetResponse::from);
    }

    @Transactional(readOnly = true)
    public MediaAssetResponse getById(Long id) {
        return MediaAssetResponse.from(findOrThrow(id));
    }

    @Transactional
    public MediaAssetResponse update(Long id, MediaUpdateRequest request) {
        MediaAsset asset = findOrThrow(id);
        asset.setAlt(request.alt());
        return MediaAssetResponse.from(mediaAssetRepository.save(asset));
    }

    /**
     * Supprime l'image chez Cloudinary puis sa fiche.
     *
     * <p>Refuse tant qu'un projet l'affiche, en nommant les projets concernés : une image
     * retirée sous les pieds du site laisserait un emplacement vide en page d'accueil ou au
     * portfolio.
     *
     * <p>Cloudinary d'abord, la fiche ensuite : dans ce sens, un échec côté Cloudinary
     * interrompt l'opération et la bibliothèque reste fidèle à ce qui est réellement
     * hébergé. L'ordre inverse laisserait un fichier facturé sans plus aucune trace ici.
     */
    @Transactional
    public void delete(Long id) {
        MediaAsset asset = findOrThrow(id);

        List<String> usedBy = projectRepository.findTitlesUsingImage(asset.getPublicId());
        if (!usedBy.isEmpty()) {
            throw new BusinessRuleException(
                    "Cette image est utilisée par " + usedBy.size()
                            + (usedBy.size() > 1 ? " projets : " : " projet : ")
                            + String.join(", ", usedBy)
                            + ". Retirez-la de ces projets avant de la supprimer.");
        }

        cloudinary.destroy(asset.getPublicId());
        mediaAssetRepository.delete(asset);
    }

    // ──────────────── Interne ────────────────

    private MediaAsset findOrThrow(Long id) {
        return mediaAssetRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Image", id));
    }

    /** « raiki/projets/abc123 » donne « raiki/projets ». */
    private static String folderOf(String publicId) {
        int lastSlash = publicId.lastIndexOf('/');
        return lastSlash <= 0 ? "" : publicId.substring(0, lastSlash);
    }
}
