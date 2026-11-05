package com.borderlessos.provider.service;

import com.borderlessos.provider.config.ProviderProperties;
import com.borderlessos.provider.entity.ProviderOutcome;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class MockProviderClient {

    private final ProviderProperties properties;

    public MockProviderClient(ProviderProperties properties) {
        this.properties = properties;
    }

    public ProviderOutcome determineScenario(String destinationCountry) {
        if (destinationCountry == null) {
            return properties.defaultScenario();
        }

        return properties.scenarios()
                .getOrDefault(
                        destinationCountry.toUpperCase(Locale.ROOT),
                        properties.defaultScenario()
                );
    }

    public String createProviderReference(UUID transferId) {
        return "MOCK-" + transferId.toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase(Locale.ROOT);
    }
}
