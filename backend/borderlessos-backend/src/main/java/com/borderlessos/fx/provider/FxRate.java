package com.borderlessos.fx.provider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record FxRate(
        String sourceCurrency,
        String destinationCurrency,
        BigDecimal rate,
        LocalDate rateDate,
        String provider
) {

    public FxRate {
        Objects.requireNonNull(sourceCurrency);
        Objects.requireNonNull(destinationCurrency);
        Objects.requireNonNull(rate);
        Objects.requireNonNull(rateDate);
        Objects.requireNonNull(provider);

        if (rate.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Exchange rate must be greater than zero"
            );
        }
    }
}