package com.raikiservices.backend.entity;

/** Cycle de vie d'un message de contact, du dépôt public jusqu'à sa qualification. */
public enum ContactStatus {
    NEW,
    IN_PROGRESS,
    CONTACTED,
    WON,
    LOST
}
