package com.borderlessos.transfer.entity;

import com.borderlessos.transfer.enums.TransferEventType;
import com.borderlessos.transfer.enums.TransferStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "transfer_audit_events")
public class TransferAuditEvent {

    @Id
    private UUID id;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email", nullable = false)
    private String actorEmail;

    @Column(name = "actor_role", nullable = false, length = 32)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private TransferEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 32)
    private TransferStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "resulting_status", nullable = false, length = 32)
    private TransferStatus resultingStatus;

    @Column(length = 1000)
    private String reason;

    @Column(name = "safe_summary", nullable = false, length = 500)
    private String safeSummary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TransferAuditEvent() {
    }

    public static TransferAuditEvent create(
            UUID transferId,
            UUID actorId,
            String actorEmail,
            String actorRole,
            TransferEventType eventType,
            TransferStatus previousStatus,
            TransferStatus resultingStatus,
            String reason,
            String safeSummary,
            Instant createdAt
    ) {
        TransferAuditEvent event = new TransferAuditEvent();
        event.id = UUID.randomUUID();
        event.transferId = transferId;
        event.actorId = actorId;
        event.actorEmail = actorEmail;
        event.actorRole = actorRole;
        event.eventType = eventType;
        event.previousStatus = previousStatus;
        event.resultingStatus = resultingStatus;
        event.reason = reason;
        event.safeSummary = safeSummary;
        event.createdAt = createdAt;
        return event;
    }
}