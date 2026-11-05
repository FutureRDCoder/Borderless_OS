package com.borderlessos.fx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Map;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "fx")
public record FxProperties(
        Provider provider,
        Frankfurter frankfurter,
        Quote quote,
        Currencies currencies,
        Map<String, AmountLimit> amountLimits,
        Map<String, Integer> minorUnits
) {

    public record Provider(
            String primary,
            String fallback
    ) {}

    public record Frankfurter(
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout,
            Duration maximumRateAge
    ) {}

    public record Quote(
            Duration validity
    ) {}

    public record Currencies(
            List<String> supported
    ) {}

    public record AmountLimit(
            BigDecimal minimum,
            BigDecimal maximum
    ) {}
}