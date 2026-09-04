package com.raikiservices.backend.entity;

import java.time.Instant;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import lombok.Data;

@Entity
@Data
public class Role {

    @Id @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String name;

    private String description;

    /** Rôle système (seedé) : non supprimable et non renommable. */
    @Column(name = "is_system", nullable = false)
    private boolean system = false;

    @Column(updatable = false)
    private Instant createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Permission> permissions;

    public Role() {}

    public Role(String name) {
        this.name = name;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
