package com.borderlessos.beneficiary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BeneficiaryCreateRequest(

        @NotBlank(message = "Recipient name is required.")
        @Size(max = 120, message = "Recipient name must not exceed 120 characters.")
        String recipientName,

        @NotBlank(message = "Destination country is required.")
        @Pattern(
                regexp = "(?i)^[A-Z]{2}$",
                message = "Destination country must be a two-letter country code."
        )
        String destinationCountry,

        @NotBlank(message = "Destination currency is required.")
        @Pattern(
                regexp = "(?i)^[A-Z]{3}$",
                message = "Destination currency must be a three-letter currency code."
        )
        String destinationCurrency,

        @NotBlank(message = "Account reference is required.")
        @Size(max = 64, message = "Account reference must not exceed 64 characters.")
        String accountReference

) {}