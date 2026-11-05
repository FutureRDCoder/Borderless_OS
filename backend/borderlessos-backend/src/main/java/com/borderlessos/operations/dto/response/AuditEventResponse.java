package com.borderlessos.operations.dto.response;

import com.borderlessos.transfer.entity.TransferAuditEvent;
import com.borderlessos.transfer.enums.TransferEventType;
import com.borderlessos.transfer.enums.TransferStatus;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        UUID transferId,
        TransferEventType eventType,
        TransferStatus previousStatus,
        TransferStatus resultingStatus,
        String actorEmail,
        String actorRole,
        String summary,
        String reason,
        Instant createdAt
) {
    public static AuditEventResponse forOperations(
            TransferAuditEvent event
    ) {
        return new AuditEventResponse(
                event.getId(),
                event.getTransferId(),
                event.getEventType(),
                event.getPreviousStatus(),
                event.getResultingStatus(),
                event.getActorEmail(),
                event.getActorRole(),
                event.getSafeSummary(),
                null,
                event.getCreatedAt()
        );
    }

    public static AuditEventResponse forCompliance(
            TransferAuditEvent event
    ) {
        return new AuditEventResponse(
                event.getId(),
                event.getTransferId(),
                event.getEventType(),
                event.getPreviousStatus(),
                event.getResultingStatus(),
                event.getActorEmail(),
                event.getActorRole(),
                event.getSafeSummary(),
                event.getReason(),
                event.getCreatedAt()
        );
    }
}
