package com.borderlessos.compliance.dto.response;

import com.borderlessos.compliance.entity.ComplianceHold;
import com.borderlessos.compliance.entity.ComplianceHoldStatus;
import com.borderlessos.transfer.entity.Transfer;
import com.borderlessos.transfer.enums.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ComplianceHoldResponse(
        UUID holdId,
        UUID transferId,
        ComplianceHoldStatus holdStatus,
        TransferStatus transferStatus,
        UUID customerId,
        String recipientName,
        String destinationCountry,
        String destinationCurrency,
        String sourceCurrency,
        BigDecimal sourceAmount,
        BigDecimal destinationAmount,
        String triggerType,
        String triggerRuleId,
        String triggerSummary,
        Instant openedAt,
        String openedByEmail,
        Instant clearedAt,
        String clearedByEmail,
        String clearanceReason,
        Instant submittedAt
) {
    public static ComplianceHoldResponse from(
            ComplianceHold hold,
            Transfer transfer
    ) {
        return new ComplianceHoldResponse(
                hold.getId(),
                hold.getTransferId(),
                hold.getStatus(),
                transfer.getStatus(),
                transfer.getCustomerId(),
                transfer.getRecipientName(),
                transfer.getDestinationCountry(),
                transfer.getDestinationCurrency(),
                transfer.getSourceCurrency(),
                transfer.getSourceAmount(),
                transfer.getDestinationAmount(),
                hold.getTriggerType(),
                hold.getTriggerRuleId(),
                hold.getTriggerSummary(),
                hold.getOpenedAt(),
                hold.getOpenedByEmail(),
                hold.getClearedAt(),
                hold.getClearedByEmail(),
                hold.getClearanceReason(),
                transfer.getSubmittedAt()
        );
    }
}
