
package com.borderlessos.fx.config;

import com.borderlessos.TestcontainersConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FxPropertiesTest {

    @Autowired
    private FxProperties fxProperties;

    @Test
    void shouldBindFxConfigurationCorrectly() {

        assertThat(fxProperties.provider().primary())
                .isEqualTo("frankfurter");

        assertThat(fxProperties.provider().fallback())
                .isEqualTo("mock");

        assertThat(fxProperties.frankfurter().baseUrl())
                .isEqualTo("https://api.frankfurter.dev/v2");

        assertThat(fxProperties.quote().validity().toMinutes())
                .isEqualTo(5);

        assertThat(fxProperties.frankfurter().maximumRateAge().toDays())
                .isEqualTo(4);

        assertThat(fxProperties.amountLimits().get("INR").minimum())
                .isEqualByComparingTo("100");

        assertThat(fxProperties.amountLimits().get("INR").maximum())
                .isEqualByComparingTo("500000");

        assertThat(fxProperties.minorUnits().get("JPY"))
                .isEqualTo(0);

        assertThat(fxProperties.currencies().supported())
                .containsExactly(
                        "INR", "USD", "EUR", "GBP",
                        "AUD", "NZD", "JPY"
                );
    }
}