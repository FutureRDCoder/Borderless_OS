package com.borderlessos.fx.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fx_quotes")
@Getter
public class FxQuote {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "source_currency", nullable = false, length = 3)
    private String sourceCurrency;

    @Column(name = "destination_currency", nullable = false, length = 3)
    private String destinationCurrency;

    @Column(name = "source_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal sourceAmount;

    @Column(name = "exchange_rate", nullable = false, precision = 24, scale = 12)
    private BigDecimal exchangeRate;

    @Column(name = "destination_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal destinationAmount;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(name = "rate_retrieved_at", nullable = false)
    private Instant rateRetrievedAt;

    @Getter
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected FxQuote() {
        // Required by JPA.
    }

    public static FxQuote create(
            UUID customerId,
            String sourceCurrency,
            String destinationCurrency,
            BigDecimal sourceAmount,
            BigDecimal exchangeRate,
            BigDecimal destinationAmount,
            String provider,
            LocalDate rateDate,
            Instant rateRetrievedAt,
            Duration validity
    ) {

        FxQuote quote = new FxQuote();

        Instant now = Instant.now();

        quote.id = UUID.randomUUID();
        quote.customerId = customerId;
        quote.sourceCurrency = sourceCurrency;
        quote.destinationCurrency = destinationCurrency;
        quote.sourceAmount = sourceAmount;
        quote.exchangeRate = exchangeRate;
        quote.destinationAmount = destinationAmount;
        quote.provider = provider;
        quote.rateDate = rateDate;
        quote.rateRetrievedAt = rateRetrievedAt;
        quote.createdAt = now;
        quote.expiresAt = now.plus(validity);

        return quote;
    }

    public void consume(Instant now) {

        if (consumedAt != null) {
            throw new IllegalStateException("FX quote has already been consumed");
        }

        if (!now.isBefore(expiresAt)) {
            throw new IllegalStateException("FX quote has expired");
        }

        consumedAt = now;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }
}
