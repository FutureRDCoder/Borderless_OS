package com.borderlessos.fx.provider;

import com.borderlessos.fx.config.FxProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

@Component("mock")
public class MockFxRateProvider implements FxRateProvider {

    private static final int RATE_SCALE = 12;

    // Synthetic educational reference rates.
    // These are NOT live market exchange rates.
    private static final Map<String, BigDecimal> INR_BASE_RATES =
            Map.of(
                    "USD", new BigDecimal("0.012"),
                    "EUR", new BigDecimal("0.011"),
                    "GBP", new BigDecimal("0.0095"),
                    "AUD", new BigDecimal("0.018"),
                    "NZD", new BigDecimal("0.020"),
                    "JPY", new BigDecimal("1.72")
            );

    private final FxProperties properties;

    public MockFxRateProvider(FxProperties properties) {
        this.properties = properties;
    }

    @Override
    public FxRate fetchRate(
            String sourceCurrency,
            String destinationCurrency
    ) {

        validateCurrencies(sourceCurrency, destinationCurrency);

        BigDecimal rate;

        if ("INR".equals(sourceCurrency)) {

            rate = INR_BASE_RATES.get(destinationCurrency);

        } else if ("INR".equals(destinationCurrency)) {

            BigDecimal inrToForeignRate =
                    INR_BASE_RATES.get(sourceCurrency);

            rate = BigDecimal.ONE.divide(
                    inrToForeignRate,
                    RATE_SCALE,
                    RoundingMode.HALF_UP
            );

        } else {

            throw new IllegalArgumentException(
                    "Unsupported currency corridor"
            );
        }

        return new FxRate(
                sourceCurrency,
                destinationCurrency,
                rate,
                LocalDate.now(),
                "mock"
        );
    }

    private void validateCurrencies(
            String sourceCurrency,
            String destinationCurrency
    ) {

        Objects.requireNonNull(sourceCurrency);
        Objects.requireNonNull(destinationCurrency);

        if (!properties.currencies().supported().contains(sourceCurrency)
                || !properties.currencies().supported()
                .contains(destinationCurrency)) {

            throw new IllegalArgumentException(
                    "Unsupported currency pair"
            );
        }

        if (sourceCurrency.equals(destinationCurrency)) {
            throw new IllegalArgumentException(
                    "Source and destination currencies must differ"
            );
        }
    }
}