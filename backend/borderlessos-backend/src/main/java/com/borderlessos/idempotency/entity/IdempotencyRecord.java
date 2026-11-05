package com.borderlessos.idempotency.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, length = 64)
    private String operation;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 128)
    private String requestFingerprint;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected IdempotencyRecord() {
    }

    public static IdempotencyRecord create(
            UUID customerId,
            String operation,
            String idempotencyKey,
            String requestFingerprint,
            UUID resourceId,
            Instant now,
            Instant expiresAt
    ) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.id = UUID.randomUUID();
        record.customerId = customerId;
        record.operation = operation;
        record.idempotencyKey = idempotencyKey;
        record.requestFingerprint = requestFingerprint;
        record.resourceId = resourceId;
        record.createdAt = now;
        record.expiresAt = expiresAt;
        return record;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
