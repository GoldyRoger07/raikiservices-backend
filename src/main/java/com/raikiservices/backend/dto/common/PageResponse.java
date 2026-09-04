package com.raikiservices.backend.dto.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Enveloppe de pagination commune à toutes les listes de l'API.
 *
 * <p>Reprend la convention du backend Secogroupe pour que les composants de tableau du
 * frontend restent interchangeables entre les deux projets.
 */
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int page,
        int size) {

    /** Convertit une {@link Page} JPA en réponse d'API, en mappant chaque élément. */
    public static <E, T> PageResponse<T> from(Page<E> source, Function<E, T> mapper) {
        return new PageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.getNumber(),
                source.getSize());
    }
}
