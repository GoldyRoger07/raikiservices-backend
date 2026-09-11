package com.raikiservices.backend.dto.media;

import java.time.Instant;

import com.raikiservices.backend.entity.MediaAsset;
import com.raikiservices.backend.entity.User;

public record MediaAssetResponse(
        Long id,
        String publicId,
        String secureUrl,
        String format,
        Integer width,
        Integer height,
        Long bytes,
        String folder,
        String originalFilename,
        String alt,
        Long uploadedById,
        String uploadedByName,
        Instant uploadedAt) {

    public static MediaAssetResponse from(MediaAsset asset) {
        User author = asset.getUploadedBy();
        return new MediaAssetResponse(
                asset.getId(),
                asset.getPublicId(),
                asset.getSecureUrl(),
                asset.getFormat(),
                asset.getWidth(),
                asset.getHeight(),
                asset.getBytes(),
                asset.getFolder(),
                asset.getOriginalFilename(),
                asset.getAlt(),
                author == null ? null : author.getId(),
                author == null ? null : displayName(author),
                asset.getUploadedAt());
    }

    private static String displayName(User author) {
        String first = author.getFirstName() == null ? "" : author.getFirstName();
        String last = author.getLastName() == null ? "" : author.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? author.getUsername() : full;
    }
}
