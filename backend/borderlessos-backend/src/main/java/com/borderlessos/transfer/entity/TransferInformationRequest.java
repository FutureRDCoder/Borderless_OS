package com.borderlessos.transfer.entity;

import com.borderlessos.common.security.CurrentActor;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "transfer_information_requests")
public class TransferInformationRequest {

    @Id
    private UUID id;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(name = "requested_by_email", nullable = false)
    private String requestedByEmail;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "responded_by")
    private UUID respondedBy;

    @Column(name = "responded_by_email")
    private String respondedByEmail;

    @Column(name = "response_message", length = 1000)
    private String responseMessage;

    protected TransferInformationRequest() {
    }

    public static TransferInformationRequest create(
            UUID transferId,
            CurrentActor actor,
            String message,
            Instant now
    ) {
        TransferInformationRequest request = new TransferInformationRequest();
        request.id = UUID.randomUUID();
        request.transferId = transferId;
        request.requestedBy = actor.id();
        request.requestedByEmail = actor.email();
        request.message = message;
        request.createdAt = now;
        return request;
    }

    public void respond(
            CurrentActor actor,
            String response,
            Instant now
    ) {
        if (respondedAt != null) {
            throw new IllegalStateException(
                    "This information request has already been answered."
            );
        }

        this.respondedAt = now;
        this.respondedBy = actor.id();
        this.respondedByEmail = actor.email();
        this.responseMessage = response;
    }
}
