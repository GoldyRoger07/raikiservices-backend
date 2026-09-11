package com.raikiservices.backend.dto.offer;

import com.raikiservices.backend.entity.LaunchOffer;

/**
 * État de l'offre de lancement.
 *
 * @param running     vrai si l'offre doit s'afficher : active et des places restantes.
 *                    C'est le seul champ dont la page publique a besoin pour trancher entre
 *                    le bloc d'offre et celui qui le remplacera ensuite
 * @param active      interrupteur général, indépendamment du compteur
 * @param claimedSlots places déjà attribuées, pour l'écran de réglage
 */
public record LaunchOfferResponse(
        boolean running,
        boolean active,
        int totalSlots,
        int claimedSlots,
        int remainingSlots) {

    public static LaunchOfferResponse from(LaunchOffer offer) {
        return new LaunchOfferResponse(
                offer.isRunning(),
                offer.isActive(),
                offer.getTotalSlots(),
                offer.getClaimedSlots(),
                offer.remainingSlots());
    }
}
