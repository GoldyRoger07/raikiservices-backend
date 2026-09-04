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
}
