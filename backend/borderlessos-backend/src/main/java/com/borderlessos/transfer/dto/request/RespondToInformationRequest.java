package com.borderlessos.transfer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RespondToInformationRequest(
        @NotBlank
        @Size(max = 1000)
        String message
) {
}
