
package com.borderlessos.fx.service;

import com.borderlessos.fx.config.FxProperties;
import com.borderlessos.fx.provider.FxRate;
import com.borderlessos.fx.provider.FxRateProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

@Service
public class FxRateProviderService {

    private final Map<String, FxRateProvider> providers;
    private final FxProperties properties;

    public FxRateProviderService(
            Map<String, FxRateProvider> providers,
            FxProperties properties
    ) {
        this.providers = providers;
        this.properties = properties;
    }

    public FxRate fetchRate(String source, String destination) {
        String primaryName = properties.provider().primary();
        String fallbackName = properties.provider().fallback();

        FxRateProvider primary = getProvider(primaryName);

        try {
            FxRate rate = primary.fetchRate(source, destination);
            validateRate(rate);
            return rate;
        } catch (RestClientException | IllegalStateException exception) {
            if (primaryName.equals(fallbackName)) {
                throw new IllegalStateException(
                        "FX provider failed.", exception);
            }

            FxRateProvider fallback = getProvider(fallbackName);

            try {
                FxRate rate = fallback.fetchRate(source, destination);
                validateRate(rate);
                return rate;
            } catch (RuntimeException fallbackException) {
                fallbackException.addSuppressed(exception);
                throw new IllegalStateException(
                        "Both FX providers failed.", fallbackException);
            }
        }
    }

    private FxRateProvider getProvider(String name) {
        FxRateProvider provider = providers.get(name);

        if (provider == null) {
            throw new IllegalStateException(
                    "Configured FX provider is unavailable.");
        }

        return provider;
    }

    private void validateRate(FxRate rate) {
        if (rate == null || rate.rate() == null || rate.rateDate() == null
                || rate.rate().signum() <= 0) {
            throw new IllegalStateException("Invalid FX rate received.");
        }

        LocalDate today = LocalDate.now();

        if (rate.rateDate().isAfter(today)) {
            throw new IllegalStateException("Future-dated FX rate received.");
        }

        if ("frankfurter".equals(rate.provider())) {
            Duration age = Duration.between(
                    rate.rateDate().atStartOfDay(),
                    today.atStartOfDay()
            );

            if (age.compareTo(
                    properties.frankfurter().maximumRateAge()) > 0) {
                throw new IllegalStateException("Stale FX rate received.");
            }
        }
    }
}
