package com.borderlessos.provider.entity;

import com.borderlessos.common.security.CurrentActor;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "provider_inquiries")
public class ProviderInquiry {

    @Id
    private UUID id;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email", nullable = false)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProviderInquiryResult result;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolved_outcome", length = 32)
    private ProviderOutcome resolvedOutcome;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProviderInquiry() {
    }

    public static ProviderInquiry create(
            UUID attemptId,
            UUID transferId,
            CurrentActor actor,
            ProviderInquiryResult result,
            ProviderOutcome resolvedOutcome,
            Instant now
    ) {
        ProviderInquiry inquiry = new ProviderInquiry();
        inquiry.id = UUID.randomUUID();
        inquiry.attemptId = attemptId;
        inquiry.transferId = transferId;
        inquiry.actorId = actor.id();
        inquiry.actorEmail = actor.email();
        inquiry.result = result;
        inquiry.resolvedOutcome = resolvedOutcome;
        inquiry.createdAt = now;
        return inquiry;
    }
}
