package com.borderlessos.operations.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectTransferRequest(
        @NotBlank
        @Size(max = 1000)
        String reason
) {
}
