package com.borderlessos.operations.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestInformationRequest(
        @NotBlank
        @Size(max = 1000)
        String message
) {
}
