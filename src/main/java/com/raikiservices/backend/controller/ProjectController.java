package com.raikiservices.backend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.raikiservices.backend.dto.common.PageQuery;
import com.raikiservices.backend.dto.common.PageResponse;
import com.raikiservices.backend.dto.project.ProjectReorderRequest;
import com.raikiservices.backend.dto.project.ProjectRequest;
import com.raikiservices.backend.dto.project.ProjectResponse;
import com.raikiservices.backend.service.ProjectService;

import jakarta.validation.Valid;

/**
 * Réalisations : face publique pour le site vitrine, face administration pour le
 * back-office.
 *
 * <p>Les listes publiques sont triées par rang d'affichage croissant — l'ordre réglé à la
 * main depuis l'administration —, là où l'administration trie par défaut sur le même champ
 * pour montrer la vitrine telle que les visiteurs la voient.
 */
@RestController
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // ──────────────── Site public : projets publiés ────────────────

    /** Portfolio. */
    @GetMapping("/public/v1/projects")
    public ResponseEntity<PageResponse<ProjectResponse>> getPublished(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "") String sortField,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(defaultValue = "") String globalFilter) {

        return ResponseEntity.ok(projectService.getPublished(
                new PageQuery(page, size, sortField, sortOrder, globalFilter)));
    }

    /** Études de cas : les projets publiés marqués comme tels, texte complet inclus. */
    @GetMapping("/public/v1/projects/case-studies")
    public ResponseEntity<PageResponse<ProjectResponse>> getCaseStudies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String sortField,
            @RequestParam(defaultValue = "asc") String sortOrder) {

        return ResponseEntity.ok(projectService.getCaseStudies(
                new PageQuery(page, size, sortField, sortOrder, "")));
    }

    /** Sélection mise en avant sur l'accueil. */
    @GetMapping("/public/v1/projects/featured")
    public ResponseEntity<PageResponse<ProjectResponse>> getFeatured(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size,
            @RequestParam(defaultValue = "") String sortField,
            @RequestParam(defaultValue = "asc") String sortOrder) {

        return ResponseEntity.ok(projectService.getFeatured(
                new PageQuery(page, size, sortField, sortOrder, "")));
    }

    /**
     * Détail d'un projet publié.
     *
     * <p>Spring préfère un chemin littéral à un paramètre, quel que soit l'ordre de
     * déclaration : {@code case-studies} et {@code featured} atteignent donc bien leurs
     * méthodes. La contrepartie est qu'un projet dont le slug vaudrait l'un de ces deux mots
     * serait inatteignable ici — le slug étant dérivé du titre, le cas reste théorique.
     */
    @GetMapping("/public/v1/projects/{slug}")
    public ResponseEntity<ProjectResponse> getPublishedBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(projectService.getPublishedBySlug(slug));
    }

    // ──────────────── Administration ────────────────

    @PreAuthorize("hasAuthority('READ_PROJECT')")
    @GetMapping("/api/v1/projects")
    public ResponseEntity<PageResponse<ProjectResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String sortField,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(defaultValue = "") String globalFilter) {

        return ResponseEntity.ok(projectService.getAll(
                new PageQuery(page, size, sortField, sortOrder, globalFilter)));
    }

    @PreAuthorize("hasAuthority('READ_PROJECT')")
    @GetMapping("/api/v1/projects/{id}")
    public ResponseEntity<ProjectResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getById(id));
    }

    @PreAuthorize("hasAuthority('CREATE_PROJECT')")
    @PostMapping("/api/v1/projects")
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }

    /**
     * Nouvel ordre de la vitrine.
     *
     * <p>Réordonner en un seul appel plutôt qu'en autant de {@code PUT} qu'il y a de projets
     * évite qu'une coupure en cours de route laisse la vitrine à moitié rangée.
     */
    @PreAuthorize("hasAuthority('UPDATE_PROJECT')")
    @PutMapping("/api/v1/projects/reorder")
    public ResponseEntity<Map<String, Integer>> reorder(
            @Valid @RequestBody ProjectReorderRequest request) {

        return ResponseEntity.ok(Map.of("reordered", projectService.reorder(request)));
    }

    @PreAuthorize("hasAuthority('UPDATE_PROJECT')")
    @PutMapping("/api/v1/projects/{id}")
    public ResponseEntity<ProjectResponse> update(@PathVariable Long id,
            @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(projectService.update(id, request));
    }

    @PreAuthorize("hasAuthority('DELETE_PROJECT')")
    @DeleteMapping("/api/v1/projects/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
