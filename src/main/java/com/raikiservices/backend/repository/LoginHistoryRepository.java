package com.raikiservices.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.raikiservices.backend.entity.LoginHistory;
import com.raikiservices.backend.entity.User;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    Page<LoginHistory> findByUserOrderByDateDesc(User user, Pageable pageable);
}
