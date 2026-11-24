package com.borderlessos.compliance.entity;

import com.borderlessos.common.security.CurrentActor;
import com.borderlessos.transfer.enums.TransferStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "compliance_decisions")
public class ComplianceDecision {

    @Id
    private UUID id;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(name = "hold_id")
    private UUID holdId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false, length = 32)
    private ComplianceDecisionType decisionType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email", nullable = false)
    private String actorEmail;

    @Column(name = "actor_role", nullable = false, length = 32)
    private String actorRole;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 32)
    private TransferStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "resulting_status", length = 32)
    private TransferStatus resultingStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ComplianceDecision() {
    }

    public static ComplianceDecision create(
            UUID transferId,
            UUID holdId,
            ComplianceDecisionType decisionType,
            CurrentActor actor,
            String reason,
            TransferStatus previousStatus,
            TransferStatus resultingStatus,
            Instant now
    ) {
        ComplianceDecision decision = new ComplianceDecision();
        decision.id = UUID.randomUUID();
        decision.transferId = transferId;
        decision.holdId = holdId;
        decision.decisionType = decisionType;
        decision.actorId = actor.id();
        decision.actorEmail = actor.email();
        decision.actorRole = actor.role();
        decision.reason = reason;
        decision.previousStatus = previousStatus;
        decision.resultingStatus = resultingStatus;
        decision.createdAt = now;
        return decision;
    }
}
