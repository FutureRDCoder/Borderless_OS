package com.borderlessos.fx.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record FxQuoteRequest(

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$")
        String sourceCurrency,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$")
        String destinationCurrency,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal sourceAmount

) {}