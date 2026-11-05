package com.borderlessos.compliance.entity;

import com.borderlessos.common.security.CurrentActor;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "compliance_holds")
public class ComplianceHold {

    @Id
    private UUID id;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ComplianceHoldStatus status;

    @Column(name = "trigger_type", nullable = false, length = 64)
    private String triggerType;

    @Column(name = "trigger_rule_id", nullable = false, length = 64)
    private String triggerRuleId;

    @Column(name = "trigger_summary", nullable = false, length = 500)
    private String triggerSummary;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "opened_by")
    private UUID openedBy;

    @Column(name = "opened_by_email", nullable = false)
    private String openedByEmail;

    @Column(name = "cleared_at")
    private Instant clearedAt;

    @Column(name = "cleared_by")
    private UUID clearedBy;

    @Column(name = "cleared_by_email")
    private String clearedByEmail;

    @Column(name = "clearance_reason", length = 1000)
    private String clearanceReason;

    @Version
    @Column(nullable = false)
    private Long version;

    protected ComplianceHold() {
    }

    public static ComplianceHold place(
            UUID transferId,
            String triggerType,
            String triggerRuleId,
            String triggerSummary,
            CurrentActor actor,
            Instant now
    ) {
        ComplianceHold hold = new ComplianceHold();
        hold.id = UUID.randomUUID();
        hold.transferId = transferId;
        hold.status = ComplianceHoldStatus.ACTIVE;
        hold.triggerType = triggerType;
        hold.triggerRuleId = triggerRuleId;
        hold.triggerSummary = triggerSummary;
        hold.openedAt = now;
        hold.openedBy = actor.id();
        hold.openedByEmail = actor.email();
        return hold;
    }

    public void clear(CurrentActor actor, String reason, Instant now) {
        if (status != ComplianceHoldStatus.ACTIVE) {
            throw new IllegalStateException("Compliance hold is not active.");
        }

        this.status = ComplianceHoldStatus.CLEARED;
        this.clearedAt = now;
        this.clearedBy = actor.id();
        this.clearedByEmail = actor.email();
        this.clearanceReason = reason;
    }
}
