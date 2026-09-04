package com.raikiservices.backend.entity;

import java.util.Optional;

/**
 * Référentiel des évènements capables d'émettre une notification.
 *
 * <p>Chaque type porte la permission qui ouvre droit à le recevoir : la diffusion cible ainsi
 * exactement les comptes habilités à voir la donnée concernée, sans liste de destinataires à
 * maintenir à la main.
 */
public enum NotificationType {

    NEW_CONTACT("Nouveau message de contact", "READ_CONTACT", "/admin/contact"),
    BLOG_PUBLISHED("Article de blog publié", "READ_BLOG", "/admin/blog"),
    NEW_USER_REGISTERED("Nouvelle inscription", "READ_USER", "/admin/users");

    private final String label;
    private final String requiredPermission;
    private final String route;

    NotificationType(String label, String requiredPermission, String route) {
        this.label = label;
        this.requiredPermission = requiredPermission;
        this.route = route;
    }

    /** Permission ouvrant droit à recevoir ce type de notification. */
    public String getRequiredPermission() {
        return requiredPermission;
    }

    /** Libellé lisible, affiché dans l'écran de configuration. */
    public String getLabel() {
        return label;
    }

    /** Route du back-office vers l'écran concerné, pour les liens des emails et du push. */
    public String getRoute() {
        return route;
    }

    public String getCode() {
        return name();
    }

    public static Optional<NotificationType> fromCode(String code) {
        for (NotificationType type : values()) {
            if (type.name().equals(code)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
