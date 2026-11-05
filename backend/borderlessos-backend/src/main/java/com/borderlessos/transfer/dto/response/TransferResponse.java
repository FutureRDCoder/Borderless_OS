package com.borderlessos.transfer.dto.response;

import com.borderlessos.transfer.entity.Transfer;
import com.borderlessos.transfer.enums.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
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
    public static TransferResponse from(Transfer transfer) {
        String accountReference = transfer.getBeneficiaryAccountReference();

        String maskedReference = accountReference.length() <= 4
                ? "****"
                : "****" + accountReference.substring(accountReference.length() - 4);

        return new TransferResponse(
                transfer.getId(),
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
