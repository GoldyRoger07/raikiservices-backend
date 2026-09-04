package com.raikiservices.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.raikiservices.backend.entity.NotificationPreference;
import com.raikiservices.backend.entity.User;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByUserAndEventType(User user, String eventType);

    List<NotificationPreference> findByUser(User user);
}
