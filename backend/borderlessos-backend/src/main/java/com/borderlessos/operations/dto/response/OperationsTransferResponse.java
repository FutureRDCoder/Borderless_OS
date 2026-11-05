package com.borderlessos.operations.dto.response;

import com.borderlessos.transfer.entity.Transfer;
import com.borderlessos.transfer.enums.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OperationsTransferResponse(
        UUID id,
        UUID customerId,
        UUID beneficiaryId,
        UUID quoteId,
        String quoteSource,
        String recipientName,
        String destinationCountry,
        String destinationCurrency,
        String maskedAccountReference,
        String sourceCurrency,
        BigDecimal sourceAmount,
        BigDecimal destinationAmount,
        BigDecimal exchangeRate,
        TransferStatus status,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static OperationsTransferResponse from(Transfer transfer) {

        String reference = transfer.getBeneficiaryAccountReference();

        String maskedReference = reference.length() <= 4
                ? "****"
                : "****" + reference.substring(reference.length() - 4);

        return new OperationsTransferResponse(
                transfer.getId(),
                transfer.getCustomerId(),
                transfer.getBeneficiaryId(),
                transfer.getFxQuoteId(),
                transfer.getQuoteSource(),
                transfer.getRecipientName(),
                transfer.getDestinationCountry(),
                transfer.getDestinationCurrency(),
                maskedReference,
                transfer.getSourceCurrency(),
                transfer.getSourceAmount(),
                transfer.getDestinationAmount(),
                transfer.getExchangeRate(),
                transfer.getStatus(),
                transfer.getSubmittedAt(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt()
        );
    }
}
