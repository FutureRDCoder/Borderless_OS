package com.borderlessos.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(
                min = 12,
                max = 128,
                message = "Password must contain between 12 and 128 characters"
        )
        String password,

        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name cannot exceed 120 characters")
        String displayName
) {
}
