package com.borderlessos.fx.service;

import com.borderlessos.fx.config.FxProperties;
import com.borderlessos.fx.provider.FxRate;
import com.borderlessos.fx.provider.FxRateProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FxRateProviderServiceTest {

    @Mock
    private FxProperties properties;

    @Mock
    private FxProperties.Provider providerProperties;

    @Mock
    private FxProperties.Frankfurter frankfurterProperties;

    @Mock
    private FxRateProvider frankfurter;

    @Mock
    private FxRateProvider mockProvider;

    private FxRateProviderService service;

    @BeforeEach
    void setUp() {
        when(properties.provider()).thenReturn(providerProperties);
        when(providerProperties.primary()).thenReturn("frankfurter");
        when(providerProperties.fallback()).thenReturn("mock");
        lenient().when(properties.frankfurter())
                .thenReturn(frankfurterProperties);

        lenient().when(frankfurterProperties.maximumRateAge())
                .thenReturn(Duration.ofDays(4));

        service = new FxRateProviderService(
                Map.of(
                        "frankfurter", frankfurter,
                        "mock", mockProvider
                ),
                properties
        );
    }

    @Test
    void shouldReturnPrimaryProviderRate() {
        FxRate expected = rate("frankfurter", LocalDate.now());

        when(frankfurter.fetchRate("INR", "JPY"))
                .thenReturn(expected);

        FxRate actual = service.fetchRate("INR", "JPY");

        assertThat(actual).isEqualTo(expected);
        verifyNoInteractions(mockProvider);
    }

    @Test
    void shouldUseFallbackWhenPrimaryFails() {
        FxRate fallbackRate = rate("mock", LocalDate.now());

        when(frankfurter.fetchRate("INR", "JPY"))
                .thenThrow(new ResourceAccessException("Connection failed"));

        when(mockProvider.fetchRate("INR", "JPY"))
                .thenReturn(fallbackRate);

        FxRate actual = service.fetchRate("INR", "JPY");

        assertThat(actual.provider()).isEqualTo("mock");
        assertThat(actual).isEqualTo(fallbackRate);
    }

    @Test
    void shouldUseFallbackWhenPrimaryRateIsStale() {
        FxRate staleRate = rate(
                "frankfurter",
                LocalDate.now().minusDays(10)
        );

        FxRate fallbackRate = rate("mock", LocalDate.now());

        when(frankfurter.fetchRate("INR", "JPY"))
                .thenReturn(staleRate);

        when(mockProvider.fetchRate("INR", "JPY"))
                .thenReturn(fallbackRate);

        FxRate actual = service.fetchRate("INR", "JPY");

        assertThat(actual.provider()).isEqualTo("mock");
    }

    @Test
    void shouldRejectFutureDatedPrimaryRateAndUseFallback() {
        FxRate futureRate = rate(
                "frankfurter",
                LocalDate.now().plusDays(1)
        );

        FxRate fallbackRate = rate("mock", LocalDate.now());

        when(frankfurter.fetchRate("INR", "JPY"))
                .thenReturn(futureRate);

        when(mockProvider.fetchRate("INR", "JPY"))
                .thenReturn(fallbackRate);

        FxRate actual = service.fetchRate("INR", "JPY");

        assertThat(actual.provider()).isEqualTo("mock");
    }

    @Test
    void shouldFailWhenBothProvidersFail() {
        when(frankfurter.fetchRate("INR", "JPY"))
                .thenThrow(new ResourceAccessException("Primary unavailable"));

        when(mockProvider.fetchRate("INR", "JPY"))
                .thenThrow(new IllegalStateException("Fallback unavailable"));

        assertThatThrownBy(() -> service.fetchRate("INR", "JPY"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Both FX providers failed.");
    }

    @Test
    void shouldRejectInvalidPrimaryRateAndUseFallback() {
        FxRate invalidRate = rate(
                "frankfurter",
                LocalDate.now()
        );

        // A non-positive rate is rejected by FxRate itself,
        // so a null response tests provider response validation.
        when(frankfurter.fetchRate("INR", "JPY"))
                .thenReturn(null);

        FxRate fallbackRate = rate("mock", LocalDate.now());

        when(mockProvider.fetchRate("INR", "JPY"))
                .thenReturn(fallbackRate);

        FxRate actual = service.fetchRate("INR", "JPY");

        assertThat(actual.provider()).isEqualTo("mock");
    }

    private FxRate rate(String provider, LocalDate date) {
        return new FxRate(
                "INR",
                "JPY",
                new BigDecimal("1.72"),
                date,
                provider
        );
    }
}