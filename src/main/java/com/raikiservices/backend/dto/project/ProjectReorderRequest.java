package com.raikiservices.backend.dto.project;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/**
 * Nouvel ordre de la vitrine : les identifiants dans l'ordre voulu, le premier en tête.
 *
 * <p>Réordonner en un seul appel plutôt qu'en autant de {@code PUT} qu'il y a de projets
 * évite qu'une coupure en cours de route laisse la vitrine à moitié rangée.
 *
 * @param ids identifiants des projets ; les projets absents de la liste gardent leur rang
 */
public record ProjectReorderRequest(
        @NotEmpty(message = "La liste des projets à réordonner est vide.")
        List<Long> ids) {
}
