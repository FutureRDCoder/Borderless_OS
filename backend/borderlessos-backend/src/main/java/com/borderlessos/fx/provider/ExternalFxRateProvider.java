package com.borderlessos.fx.provider;

import com.borderlessos.fx.config.FxProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Objects;

@Component("frankfurter")
public class ExternalFxRateProvider implements FxRateProvider {

    private final RestClient restClient;
    private final FxProperties properties;

    public ExternalFxRateProvider(
            RestClient fxRestClient,
            FxProperties properties
    ) {
        this.restClient = fxRestClient;
        this.properties = properties;
    }

    @Override
    public FxRate fetchRate(
            String sourceCurrency,
            String destinationCurrency
    ) {

        validateCurrencies(sourceCurrency, destinationCurrency);

        FrankfurterRateResponse response = restClient.get()
                .uri("/rate/{source}/{destination}",
                        sourceCurrency,
                        destinationCurrency)
                .retrieve()
                .body(FrankfurterRateResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Frankfurter returned an empty response"
            );
        }

        return new FxRate(
                response.base(),
                response.quote(),
                response.rate(),
                response.date(),
                "frankfurter"
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

    private record FrankfurterRateResponse(
            LocalDate date,
            String base,
            String quote,
            java.math.BigDecimal rate
    ) {}
}