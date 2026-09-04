package com.raikiservices.backend.exception;

/** Règle métier violée (slug déjà pris, email déjà inscrit…) — traduite en 409. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
