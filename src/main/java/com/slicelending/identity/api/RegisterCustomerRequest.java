package com.slicelending.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterCustomerRequest(
        @Email @NotBlank @Size(max = 255)
        String email,
        @NotBlank @Size(max = 72, min = 12)
        String password,
        @NotBlank@Size( max = 100)
        String firstName,
        @NotBlank @Size(max = 100)
        String lastName,
        @NotBlank @Size(max = 20)
        @Pattern(
                regexp = "^\\+?[1-9]\\d{7,14}$",
                message = "Phone number must be a valid international number"
        )
        String phoneNumber

        // A record is a short, immutable DTO. Java automatically provides:
        // It does not create setters, so the request cannot be changed after creation.
) {
}
