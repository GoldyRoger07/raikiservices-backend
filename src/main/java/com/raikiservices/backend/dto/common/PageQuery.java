package com.raikiservices.backend.dto.common;

import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Paramètres de pagination reçus en query string : {@code page}, {@code size},
 * {@code sortField}, {@code sortOrder}, {@code globalFilter}.
 */
public record PageQuery(int page, int size, String sortField, String sortOrder, String globalFilter) {

    private static final int MAX_SIZE = 100;

    /**
     * Construit le {@link Pageable} correspondant.
     *
     * @param sortableFields champs autorisés au tri. Un {@code sortField} absent de cette
     *                       liste est ignoré au profit de {@code defaultField} : le nom
     *                       arrive du client et finirait sinon dans une clause SQL ORDER BY.
     */
    public Pageable toPageable(Set<String> sortableFields, String defaultField) {
        String field = (sortField != null && sortableFields.contains(sortField)) ? sortField : defaultField;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_SIZE),
                Sort.by(direction, field));
    }

    /** Filtre texte normalisé, ou {@code null} s'il est vide. */
    public String filterOrNull() {
        return (globalFilter == null || globalFilter.isBlank()) ? null : globalFilter.trim();
    }
}
