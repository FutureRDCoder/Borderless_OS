package com.borderlessos.compliance.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "borderlessos.compliance")
public record ComplianceProperties(
        @NotEmpty List<String> holdCountries,
        @NotBlank String ruleId
) {
}
