package com.raikiservices.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.raikiservices.backend.entity.ContactMessage;
import com.raikiservices.backend.entity.ContactStatus;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    long countByStatus(ContactStatus status);

    @Query("""
            SELECT c FROM ContactMessage c WHERE
            LOWER(c.firstName)       LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(c.lastName)        LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(c.email)           LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(c.companyName)     LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(c.serviceCategory) LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(c.subject)         LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(c.message)         LIKE LOWER(CONCAT('%', :f, '%'))
            """)
    Page<ContactMessage> search(@Param("f") String filter, Pageable pageable);

    Page<ContactMessage> findByStatus(ContactStatus status, Pageable pageable);

    /**
     * Recherche texte restreinte a un statut : le back-office combine les deux filtres.
     *
     * <p>Le statut n'est volontairement pas couvert par {@link #search} : c'est une valeur
     * d'enumeration, pas du texte libre, et la comparer en LIKE ferait remonter des messages
     * dont le corps contient par hasard le mot cherche.
     */
    @Query("""
            SELECT c FROM ContactMessage c WHERE c.status = :status AND (
            LOWER(c.firstName)       LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(c.lastName)        LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(c.email)           LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(c.companyName)     LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(c.serviceCategory) LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(c.subject)         LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(c.message)         LIKE LOWER(CONCAT('%', :f, '%')))
            """)
    Page<ContactMessage> searchByStatus(@Param("f") String filter,
            @Param("status") ContactStatus status, Pageable pageable);
}
