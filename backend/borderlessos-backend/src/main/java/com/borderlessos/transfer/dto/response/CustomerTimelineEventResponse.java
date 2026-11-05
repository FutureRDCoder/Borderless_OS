package com.borderlessos.transfer.dto.response;

import com.borderlessos.transfer.entity.TransferAuditEvent;
import com.borderlessos.transfer.enums.TransferEventType;
import com.borderlessos.transfer.enums.TransferStatus;

import java.time.Instant;
import java.util.UUID;

public record CustomerTimelineEventResponse(
        UUID id,
        TransferEventType eventType,
        TransferStatus previousStatus,
        TransferStatus resultingStatus,
        String summary,
        Instant createdAt
) {
    public static CustomerTimelineEventResponse from(
            TransferAuditEvent event
    ) {
        return new CustomerTimelineEventResponse(
                event.getId(),
                event.getEventType(),
                event.getPreviousStatus(),
                event.getResultingStatus(),
                event.getSafeSummary(),
                event.getCreatedAt()
        );
    }
}
