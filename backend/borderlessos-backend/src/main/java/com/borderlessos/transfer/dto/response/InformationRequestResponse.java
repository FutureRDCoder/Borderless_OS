package com.borderlessos.transfer.dto.response;

import com.borderlessos.transfer.entity.TransferInformationRequest;

import java.time.Instant;
import java.util.UUID;

public record InformationRequestResponse(
        UUID id,
        UUID transferId,
        String message,
        String requestedByEmail,
        Instant createdAt,
        Instant respondedAt,
        String respondedByEmail,
        String responseMessage
) {
    public static InformationRequestResponse from(
            TransferInformationRequest request
    ) {
        return new InformationRequestResponse(
                request.getId(),
                request.getTransferId(),
                request.getMessage(),
                request.getRequestedByEmail(),
                request.getCreatedAt(),
                request.getRespondedAt(),
                request.getRespondedByEmail(),
                request.getResponseMessage()
        );
    }
}
