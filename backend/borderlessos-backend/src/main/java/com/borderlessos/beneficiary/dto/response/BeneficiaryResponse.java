package com.borderlessos.beneficiary.dto.response;

import com.borderlessos.beneficiary.entity.Beneficiary;

import java.time.Instant;
import java.util.UUID;

public record BeneficiaryResponse(

        UUID id,

        String recipientName,

        String destinationCountry,

        String destinationCurrency,

        String accountReference,

        boolean active,

        Instant createdAt,

        Instant updatedAt

) {

    public static BeneficiaryResponse from(Beneficiary beneficiary) {

        return new BeneficiaryResponse(
                beneficiary.getId(),
                beneficiary.getRecipientName(),
                beneficiary.getDestinationCountry().trim(),
                beneficiary.getDestinationCurrency().trim(),
                beneficiary.getAccountReference(),
                beneficiary.isActive(),
                beneficiary.getCreatedAt(),
                beneficiary.getUpdatedAt()
        );
    }
}