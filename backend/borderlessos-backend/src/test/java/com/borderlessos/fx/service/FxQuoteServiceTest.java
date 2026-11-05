package com.borderlessos.fx.service;

import com.borderlessos.fx.config.FxProperties;
import com.borderlessos.fx.dto.request.FxQuoteRequest;
import com.borderlessos.fx.dto.response.FxQuoteResponse;
import com.borderlessos.fx.entity.FxQuote;
import com.borderlessos.fx.provider.FxRate;
import com.borderlessos.fx.repository.FxQuoteRepository;
import com.borderlessos.identity.entity.User;
import com.borderlessos.identity.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FxQuoteServiceTest {

    private static final String CUSTOMER_EMAIL = "customer@example.com";

    @Mock
    private FxQuoteRepository quoteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FxRateProviderService rateProviderService;

    @Mock
    private FxProperties properties;

    @Mock
    private FxProperties.Currencies currencies;

    @Mock
    private FxProperties.Quote quoteProperties;

    @Mock
    private User user;

    private FxQuoteService service;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();

        lenient().when(userRepository.findByEmail(CUSTOMER_EMAIL))
                .thenReturn(Optional.of(user));
        lenient().when(user.getId()).thenReturn(customerId);

        lenient().when(properties.currencies()).thenReturn(currencies);
        lenient().when(currencies.supported()).thenReturn(
                List.of("INR", "USD", "EUR", "GBP", "AUD", "NZD", "JPY")
        );

        lenient().when(properties.amountLimits()).thenReturn(Map.of(
                "INR", new FxProperties.AmountLimit(
                        new BigDecimal("100"),
                        new BigDecimal("500000")
                ),
                "USD", new FxProperties.AmountLimit(
                        new BigDecimal("1.20"),
                        new BigDecimal("6000")
                ),
                "JPY", new FxProperties.AmountLimit(
                        new BigDecimal("172"),
                        new BigDecimal("860000")
                )
        ));

        lenient().when(properties.minorUnits()).thenReturn(Map.of(
                "INR", 2,
                "USD", 2,
                "EUR", 2,
                "GBP", 2,
                "AUD", 2,
                "NZD", 2,
                "JPY", 0
        ));

        lenient().when(properties.quote()).thenReturn(quoteProperties);
        lenient().when(quoteProperties.validity()).thenReturn(Duration.ofMinutes(5));

        lenient().when(quoteRepository.save(any(FxQuote.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        CUSTOMER_EMAIL, "password", List.of()
                )
        );

        service = new FxQuoteService(
                quoteRepository,
                userRepository,
                rateProviderService,
                properties
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateAndPersistQuoteSuccessfully() {
        when(rateProviderService.fetchRate("INR", "JPY"))
                .thenReturn(rate("1.72"));

        FxQuoteResponse response = service.createQuote(
                request("INR", "JPY", "1000")
        );

        assertThat(response.quoteId()).isNotNull();
        assertThat(response.sourceCurrency()).isEqualTo("INR");
        assertThat(response.destinationCurrency()).isEqualTo("JPY");
        assertThat(response.sourceAmount()).isEqualByComparingTo("1000");
        assertThat(response.destinationAmount()).isEqualByComparingTo("1720");
        assertThat(response.provider()).isEqualTo("frankfurter");
        assertThat(response.expired()).isFalse();
        assertThat(response.consumed()).isFalse();

        verify(quoteRepository).save(any(FxQuote.class));
    }

    @Test
    void shouldCalculateDestinationAmountUsingHalfUpRounding() {
        when(rateProviderService.fetchRate("INR", "JPY"))
                .thenReturn(rate("1.725"));

        FxQuoteResponse response = service.createQuote(
                request("INR", "JPY", "100")
        );

        assertThat(response.destinationAmount())
                .isEqualByComparingTo("173");
    }

    @Test
    void shouldSupportReverseCurrencyConversion() {
        when(rateProviderService.fetchRate("USD", "INR"))
                .thenReturn(new FxRate(
                        "USD",
                        "INR",
                        new BigDecimal("83.50"),
                        LocalDate.now(),
                        "frankfurter"
                ));

        FxQuoteResponse response = service.createQuote(
                request("USD", "INR", "100")
        );

        assertThat(response.destinationCurrency()).isEqualTo("INR");
        assertThat(response.destinationAmount())
                .isEqualByComparingTo("8350.00");
    }

    @Test
    void shouldRejectUnsupportedCurrency() {
        assertThatThrownBy(() -> service.createQuote(
                request("CAD", "INR", "100")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported currency");

        verifyNoInteractions(rateProviderService);
        verify(quoteRepository, never()).save(any());
    }

    @Test
    void shouldRejectSameCurrencyPair() {
        assertThatThrownBy(() -> service.createQuote(
                request("INR", "INR", "100")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source and destination currencies must differ");
    }

    @Test
    void shouldRejectNonInrCrossCurrencyPair() {
        assertThatThrownBy(() -> service.createQuote(
                request("USD", "EUR", "100")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only INR bilateral currency corridors are supported");
    }

    @Test
    void shouldRejectAmountBelowMinimum() {
        assertThatThrownBy(() -> service.createQuote(
                request("INR", "JPY", "99")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source amount is outside the permitted range");
    }

    @Test
    void shouldRejectAmountAboveMaximum() {
        assertThatThrownBy(() -> service.createQuote(
                request("INR", "JPY", "500001")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source amount is outside the permitted range");
    }

    @Test
    void shouldRejectExcessiveCurrencyPrecision() {
        assertThatThrownBy(() -> service.createQuote(
                request("INR", "JPY", "100.123")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source amount exceeds the permitted currency precision");
    }

    @Test
    void shouldRejectMissingAuthenticatedCustomer() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> service.createQuote(
                request("INR", "JPY", "100")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Authentication is required");
    }

    @Test
    void shouldRejectAuthenticatedUserNotFound() {
        when(userRepository.findByEmail(CUSTOMER_EMAIL))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createQuote(
                request("INR", "JPY", "100")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Authenticated customer not found");
    }

    @Test
    void shouldNotPersistQuoteWhenRateProviderFails() {
        when(rateProviderService.fetchRate("INR", "JPY"))
                .thenThrow(new IllegalStateException("Both FX providers failed."));

        assertThatThrownBy(() -> service.createQuote(
                request("INR", "JPY", "100")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Both FX providers failed.");

        verify(quoteRepository, never()).save(any());
    }

    private FxQuoteRequest request(
            String source,
            String destination,
            String amount
    ) {
        return new FxQuoteRequest(
                source,
                destination,
                new BigDecimal(amount)
        );
    }

    private FxRate rate(String value) {
        return new FxRate(
                "INR",
                "JPY",
                new BigDecimal(value),
                LocalDate.now(),
                "frankfurter"
        );
    }
}