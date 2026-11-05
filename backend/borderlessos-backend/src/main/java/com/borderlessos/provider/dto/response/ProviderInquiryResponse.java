package com.borderlessos.provider.dto.response;

import com.borderlessos.provider.entity.ProviderInquiry;
import com.borderlessos.provider.entity.ProviderInquiryResult;
import com.borderlessos.provider.entity.ProviderOutcome;

import java.time.Instant;
import java.util.UUID;

public record ProviderInquiryResponse(
        UUID id,
        ProviderInquiryResult result,
        ProviderOutcome resolvedOutcome,
        String actorEmail,
        Instant createdAt
) {
    public static ProviderInquiryResponse from(ProviderInquiry inquiry) {
        return new ProviderInquiryResponse(
                inquiry.getId(),
                inquiry.getResult(),
                inquiry.getResolvedOutcome(),
                inquiry.getActorEmail(),
                inquiry.getCreatedAt()
        );
    }
}
