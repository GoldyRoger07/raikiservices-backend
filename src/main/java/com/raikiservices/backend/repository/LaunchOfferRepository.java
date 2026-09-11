package com.raikiservices.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.raikiservices.backend.entity.LaunchOffer;

public interface LaunchOfferRepository extends JpaRepository<LaunchOffer, Long> {
}
