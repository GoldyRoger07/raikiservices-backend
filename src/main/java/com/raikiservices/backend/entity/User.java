package com.raikiservices.backend.entity;
import java.time.Instant;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import lombok.Data;

@Entity
@Data
public class User {
    @Id
    @GeneratedValue
    private Long id;

    private String username;
    private String password;
    private String email;

    /**
     * Faux tant que l'adresse email n'a pas ete verifiee : un compte non verifie ne peut pas
     * se connecter. Bascule a vrai a la validation de l'OTP ou du lien.
     */
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    // Profile fields
    private String firstName;
    private String lastName;
    private String phone;
    private String position;
    @Column(length = 500)
    private String bio;
    private String photoUrl;

    @Column(updatable = false)
    private Instant createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Role> roles;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
