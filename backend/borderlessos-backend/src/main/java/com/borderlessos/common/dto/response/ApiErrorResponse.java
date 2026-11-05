package com.borderlessos.common.dto.response;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String error,
        String message,
        String path,
        String requestId,
        Map<String, String> validationErrors
) {
}
