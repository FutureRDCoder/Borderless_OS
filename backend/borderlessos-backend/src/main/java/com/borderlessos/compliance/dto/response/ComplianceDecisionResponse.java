package com.borderlessos.compliance.dto.response;

import com.borderlessos.compliance.entity.ComplianceDecision;
import com.borderlessos.compliance.entity.ComplianceDecisionType;
import com.borderlessos.transfer.enums.TransferStatus;

import java.time.Instant;
import java.util.UUID;

public record ComplianceDecisionResponse(
        UUID id,
        UUID transferId,
        UUID holdId,
        ComplianceDecisionType decisionType,
        String actorEmail,
        String actorRole,
        String reason,
        TransferStatus previousStatus,
        TransferStatus resultingStatus,
        Instant createdAt
) {
    public static ComplianceDecisionResponse from(
            ComplianceDecision decision
    ) {
        return new ComplianceDecisionResponse(
                decision.getId(),
                decision.getTransferId(),
                decision.getHoldId(),
                decision.getDecisionType(),
                decision.getActorEmail(),
                decision.getActorRole(),
                decision.getReason(),
                decision.getPreviousStatus(),
                decision.getResultingStatus(),
                decision.getCreatedAt()
        );
    }
}
