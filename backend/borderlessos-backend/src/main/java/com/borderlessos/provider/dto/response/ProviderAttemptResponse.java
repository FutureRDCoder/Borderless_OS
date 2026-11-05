package com.borderlessos.provider.dto.response;

import com.borderlessos.provider.entity.ProviderAttempt;
import com.borderlessos.provider.entity.ProviderAttemptStatus;
import com.borderlessos.provider.entity.ProviderInquiry;
import com.borderlessos.provider.entity.ProviderOutcome;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProviderAttemptResponse(
        UUID attemptId,
        UUID transferId,
        String provider,
        String providerReference,
        ProviderAttemptStatus status,
        ProviderOutcome scenario,
        ProviderOutcome pendingOutcome,
        Instant createdAt,
        Instant updatedAt,
        Instant lastInquiryAt,
        List<ProviderInquiryResponse> inquiries
) {
    public static ProviderAttemptResponse from(
            ProviderAttempt attempt,
            List<ProviderInquiry> inquiries
    ) {
        return new ProviderAttemptResponse(
                attempt.getId(),
                attempt.getTransferId(),
                attempt.getProvider(),
                attempt.getProviderReference(),
                attempt.getStatus(),
                attempt.getScenario(),
                attempt.getPendingOutcome(),
                attempt.getCreatedAt(),
                attempt.getUpdatedAt(),
                attempt.getLastInquiryAt(),
                inquiries.stream()
                        .map(ProviderInquiryResponse::from)
                        .toList()
        );
    }
}
