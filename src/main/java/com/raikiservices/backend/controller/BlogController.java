package com.raikiservices.backend.controller;

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

import com.raikiservices.backend.dto.blog.BlogPostRequest;
import com.raikiservices.backend.dto.blog.BlogPostResponse;
import com.raikiservices.backend.dto.common.PageQuery;
import com.raikiservices.backend.dto.common.PageResponse;
import com.raikiservices.backend.service.BlogService;

import jakarta.validation.Valid;

@RestController
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    // ──────────────── Site public : articles publiés ────────────────

    @GetMapping("/public/v1/blog")
    public ResponseEntity<PageResponse<BlogPostResponse>> getPublished(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "") String globalFilter) {

        return ResponseEntity.ok(blogService.getPublished(
                new PageQuery(page, size, sortField, sortOrder, globalFilter)));
    }

    @GetMapping("/public/v1/blog/{slug}")
    public ResponseEntity<BlogPostResponse> getPublishedBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(blogService.getPublishedBySlug(slug));
    }

    // ──────────────── Administration ────────────────

    @PreAuthorize("hasAuthority('READ_BLOG')")
    @GetMapping("/api/v1/blog")
    public ResponseEntity<PageResponse<BlogPostResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "") String globalFilter) {

        return ResponseEntity.ok(blogService.getAll(
                new PageQuery(page, size, sortField, sortOrder, globalFilter)));
    }

    @PreAuthorize("hasAuthority('READ_BLOG')")
    @GetMapping("/api/v1/blog/{id}")
    public ResponseEntity<BlogPostResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(blogService.getById(id));
    }

    @PreAuthorize("hasAuthority('CREATE_BLOG')")
    @PostMapping("/api/v1/blog")
    public ResponseEntity<BlogPostResponse> create(@Valid @RequestBody BlogPostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(blogService.create(request));
    }

    @PreAuthorize("hasAuthority('UPDATE_BLOG')")
    @PutMapping("/api/v1/blog/{id}")
    public ResponseEntity<BlogPostResponse> update(@PathVariable Long id,
            @Valid @RequestBody BlogPostRequest request) {
        return ResponseEntity.ok(blogService.update(id, request));
    }

    @PreAuthorize("hasAuthority('DELETE_BLOG')")
    @DeleteMapping("/api/v1/blog/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        blogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
