package com.borderlessos.transfer.entity;

import com.borderlessos.transfer.enums.TransferStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "transfers")
public class Transfer {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "beneficiary_id", nullable = false)
    private UUID beneficiaryId;

    @Column(name = "fx_quote_id", nullable = false)
    private UUID fxQuoteId;

    @Column(name = "quote_source", nullable = false, length = 32)
    private String quoteSource;

    @Column(name = "recipient_name", nullable = false, length = 120)
    private String recipientName;

    @Column(name = "destination_country", nullable = false, length = 2)
    private String destinationCountry;

    @Column(name = "destination_currency", nullable = false, length = 3)
    private String destinationCurrency;

    @Column(name = "beneficiary_account_reference",
            nullable = false, length = 120)
    private String beneficiaryAccountReference;

    @Column(name = "source_currency", nullable = false, length = 3)
    private String sourceCurrency;

    @Column(name = "source_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal sourceAmount;

    @Column(name = "destination_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal destinationAmount;

    @Column(name = "exchange_rate", nullable = false, precision = 24, scale = 12)
    private BigDecimal exchangeRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransferStatus status;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Transfer() {
    }

    public static Transfer draft(
            UUID customerId,
            UUID beneficiaryId,
            UUID fxQuoteId,
            String quoteSource,
            String recipientName,
            String destinationCountry,
            String destinationCurrency,
            String beneficiaryAccountReference,
            String sourceCurrency,
            BigDecimal sourceAmount,
            BigDecimal destinationAmount,
            BigDecimal exchangeRate,
            Instant now
    ) {
        Transfer transfer = new Transfer();

        transfer.id = UUID.randomUUID();
        transfer.customerId = customerId;
        transfer.beneficiaryId = beneficiaryId;
        transfer.fxQuoteId = fxQuoteId;
        transfer.quoteSource = quoteSource;

        transfer.recipientName = recipientName;
        transfer.destinationCountry = destinationCountry;
        transfer.destinationCurrency = destinationCurrency;
        transfer.beneficiaryAccountReference = beneficiaryAccountReference;

        transfer.sourceCurrency = sourceCurrency;
        transfer.sourceAmount = sourceAmount;
        transfer.destinationAmount = destinationAmount;
        transfer.exchangeRate = exchangeRate;

        transfer.status = TransferStatus.DRAFT;
        transfer.submittedAt = null;
        transfer.createdAt = now;
        transfer.updatedAt = now;

        return transfer;
    }

    public void refreshDraft(
            UUID beneficiaryId,
            UUID fxQuoteId,
            String quoteSource,
            String recipientName,
            String destinationCountry,
            String destinationCurrency,
            String beneficiaryAccountReference,
            String sourceCurrency,
            BigDecimal sourceAmount,
            BigDecimal destinationAmount,
            BigDecimal exchangeRate,
            Instant now
    ) {
        if (status != TransferStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only a DRAFT transfer can be edited."
            );
        }

        this.beneficiaryId = beneficiaryId;
        this.fxQuoteId = fxQuoteId;
        this.quoteSource = quoteSource;
        this.recipientName = recipientName;
        this.destinationCountry = destinationCountry;
        this.destinationCurrency = destinationCurrency;
        this.beneficiaryAccountReference = beneficiaryAccountReference;
        this.sourceCurrency = sourceCurrency;
        this.sourceAmount = sourceAmount;
        this.destinationAmount = destinationAmount;
        this.exchangeRate = exchangeRate;
        this.updatedAt = now;
    }

    public void markSubmitted(Instant now) {
        this.submittedAt = now;
        this.updatedAt = now;
    }

    public void transitionTo(TransferStatus nextStatus, Instant now) {
        this.status = nextStatus;
        this.updatedAt = now;
    }
}
