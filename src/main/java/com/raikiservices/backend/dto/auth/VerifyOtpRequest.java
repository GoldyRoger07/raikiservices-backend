package com.raikiservices.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank(message = "L'adresse email est obligatoire.")
        @Email(message = "L'adresse email n'est pas valide.")
        String email,

        @NotBlank(message = "Le code est obligatoire.")
        @Pattern(regexp = "[0-9]{6}", message = "Le code doit comporter 6 chiffres.")
        String otpCode) {
}
