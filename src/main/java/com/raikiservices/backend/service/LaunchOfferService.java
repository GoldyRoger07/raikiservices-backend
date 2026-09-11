package com.raikiservices.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raikiservices.backend.dto.offer.LaunchOfferRequest;
import com.raikiservices.backend.dto.offer.LaunchOfferResponse;
import com.raikiservices.backend.entity.LaunchOffer;
import com.raikiservices.backend.exception.BusinessRuleException;
import com.raikiservices.backend.repository.LaunchOfferRepository;

/**
 * Offre de lancement.
 *
 * <p>Une seule ligne, créée à la volée au premier accès plutôt que par le seeder : la page
 * des tarifs interroge cette ressource à chaque visite et ne doit jamais dépendre de l'ordre
 * de démarrage des composants.
 */
@Service
public class LaunchOfferService {

    private final LaunchOfferRepository launchOfferRepository;

    public LaunchOfferService(LaunchOfferRepository launchOfferRepository) {
        this.launchOfferRepository = launchOfferRepository;
    }

    @Transactional
    public LaunchOfferResponse get() {
        return LaunchOfferResponse.from(findOrCreate());
    }

    @Transactional
    public LaunchOfferResponse update(LaunchOfferRequest request) {
        // Plus de places attribuées que de places ouvertes n'a pas de sens, et donnerait un
        // « il reste -2 places » à la première erreur de frappe.
        if (request.claimedSlots() > request.totalSlots()) {
            throw new BusinessRuleException(
                    "Le nombre de places attribuées ne peut pas dépasser le nombre de places ouvertes.");
        }

        LaunchOffer offer = findOrCreate();
        offer.setActive(Boolean.TRUE.equals(request.active()));
        offer.setTotalSlots(request.totalSlots());
        offer.setClaimedSlots(request.claimedSlots());

        return LaunchOfferResponse.from(launchOfferRepository.save(offer));
    }

    /**
     * La ligne unique, créée avec ses valeurs par défaut si elle n'existe pas encore.
     *
     * <p>On prend la première ligne trouvée plutôt que l'identifiant fixe : l'identifiant est
     * attribué par la base, et forcer une valeur entrerait en conflit avec le générateur.
     */
    private LaunchOffer findOrCreate() {
        return launchOfferRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> launchOfferRepository.save(new LaunchOffer()));
    }
}
