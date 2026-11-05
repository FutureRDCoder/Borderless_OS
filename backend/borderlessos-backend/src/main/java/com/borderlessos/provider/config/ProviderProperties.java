package com.borderlessos.provider.config;

import com.borderlessos.provider.entity.ProviderOutcome;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "borderlessos.provider")
public record ProviderProperties(
        String name,
        Map<String, ProviderOutcome> scenarios,
        ProviderOutcome defaultScenario
) {
}
