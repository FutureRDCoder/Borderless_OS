package com.borderlessos.provider.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "provider_attempts")
public class ProviderAttempt {

    @Id
    private UUID id;

    @Column(name = "transfer_id", nullable = false, unique = true)
    private UUID transferId;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "provider_reference", nullable = false, unique = true,
            length = 128)
    private String providerReference;

    @Column(name = "idempotency_key", nullable = false, unique = true,
            length = 128)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 128)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProviderAttemptStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private ProviderOutcome scenario;

    @Enumerated(EnumType.STRING)
    @Column(name = "pending_outcome", length = 32)
    private ProviderOutcome pendingOutcome;

    @Column(name = "last_inquiry_at")
    private Instant lastInquiryAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected ProviderAttempt() {
    }

    public static ProviderAttempt start(
            UUID transferId,
            String provider,
            String providerReference,
            String idempotencyKey,
            String requestFingerprint,
            ProviderOutcome scenario,
            Instant now
    ) {
        ProviderAttempt attempt = new ProviderAttempt();
        attempt.id = UUID.randomUUID();
        attempt.transferId = transferId;
        attempt.provider = provider;
        attempt.providerReference = providerReference;
        attempt.idempotencyKey = idempotencyKey;
        attempt.requestFingerprint = requestFingerprint;
        attempt.status = ProviderAttemptStatus.PROCESSING;
        attempt.scenario = scenario;
        attempt.createdAt = now;
        attempt.updatedAt = now;
        return attempt;
    }

    public void markCompleted(Instant now) {
        this.status = ProviderAttemptStatus.COMPLETED;
        this.updatedAt = now;
    }

    public void markFailed(Instant now) {
        this.status = ProviderAttemptStatus.FAILED;
        this.updatedAt = now;
    }

    public void markUnknown(ProviderOutcome pendingOutcome, Instant now) {
        this.status = ProviderAttemptStatus.UNKNOWN;
        this.pendingOutcome = pendingOutcome;
        this.updatedAt = now;
    }

    public void recordInquiry(Instant now) {
        this.lastInquiryAt = now;
        this.updatedAt = now;
    }

    public void resolvePending(ProviderOutcome outcome, Instant now) {
        this.status = switch (outcome) {
            case SUCCESS -> ProviderAttemptStatus.COMPLETED;
            case FAILURE -> ProviderAttemptStatus.FAILED;
            default -> ProviderAttemptStatus.UNKNOWN;
        };
        this.updatedAt = now;
    }
}
