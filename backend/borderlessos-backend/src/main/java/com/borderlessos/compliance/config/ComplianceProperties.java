package com.borderlessos.compliance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "borderlessos.compliance")
public record ComplianceProperties(
        List<String> holdCountries,
        String ruleId
) {
}
