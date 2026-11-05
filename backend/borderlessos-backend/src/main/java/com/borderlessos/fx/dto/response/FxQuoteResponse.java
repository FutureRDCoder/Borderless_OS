package com.borderlessos.fx.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FxQuoteResponse(

        UUID quoteId,
        String sourceCurrency,
        String destinationCurrency,
        BigDecimal sourceAmount,
        BigDecimal exchangeRate,
        BigDecimal destinationAmount,
        String provider,
        LocalDate rateDate,
        Instant createdAt,
        Instant expiresAt,
        boolean expired,
        boolean consumed

) {}
