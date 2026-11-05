package com.borderlessos.fx.service;

import com.borderlessos.fx.config.FxProperties;
import com.borderlessos.fx.dto.request.FxQuoteRequest;
import com.borderlessos.fx.dto.response.FxQuoteResponse;
import com.borderlessos.fx.entity.FxQuote;
import com.borderlessos.fx.provider.FxRate;
import com.borderlessos.fx.repository.FxQuoteRepository;
import com.borderlessos.identity.entity.User;
import com.borderlessos.identity.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class FxQuoteService {

    private final FxQuoteRepository quoteRepository;
    private final UserRepository userRepository;
    private final FxRateProviderService rateProviderService;
    private final FxProperties properties;

    public FxQuoteService(
            FxQuoteRepository quoteRepository,
            UserRepository userRepository,
            FxRateProviderService rateProviderService,
            FxProperties properties
    ) {
        this.quoteRepository = quoteRepository;
        this.userRepository = userRepository;
        this.rateProviderService = rateProviderService;
        this.properties = properties;
    }

    @Transactional
    public FxQuoteResponse createQuote(FxQuoteRequest request) {

        UUID customerId = getAuthenticatedCustomerId();

        String source = request.sourceCurrency().toUpperCase(Locale.ROOT);
        String destination = request.destinationCurrency().toUpperCase(Locale.ROOT);
        BigDecimal amount = request.sourceAmount();

        validateCurrencyPair(source, destination);
        validateAmount(source, amount);

        FxRate rate = rateProviderService.fetchRate(source, destination);

        int destinationScale = properties.minorUnits().get(destination);

        BigDecimal destinationAmount = amount
                .multiply(rate.rate())
                .setScale(destinationScale, RoundingMode.HALF_UP);

        Instant retrievedAt = Instant.now();

        FxQuote quote = FxQuote.create(
                customerId,
                source,
                destination,
                amount,
                rate.rate(),
                destinationAmount,
                rate.provider(),
                rate.rateDate(),
                retrievedAt,
                properties.quote().validity()
        );

        FxQuote savedQuote = quoteRepository.save(quote);

        return toResponse(savedQuote);
    }

    @Transactional(readOnly = true)
    public FxQuoteResponse getQuote(UUID quoteId) {

        UUID customerId = getAuthenticatedCustomerId();

        FxQuote quote = quoteRepository
                .findByIdAndCustomerId(quoteId, customerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "FX quote not found."
                ));

        return toResponse(quote);
    }

    private UUID getAuthenticatedCustomerId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().equals("anonymousUser")) {
            throw new IllegalStateException("Authentication is required");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException("Authenticated customer not found"));

        return user.getId();
    }

    private void validateCurrencyPair(String source, String destination) {

        if (!properties.currencies().supported().contains(source)
                || !properties.currencies().supported().contains(destination)) {
            throw new IllegalArgumentException("Unsupported currency");
        }

        if (source.equals(destination)) {
            throw new IllegalArgumentException(
                    "Source and destination currencies must differ");
        }

        // Only the six approved INR bilateral corridors are supported.
        if (!source.equals("INR") && !destination.equals("INR")) {
            throw new IllegalArgumentException(
                    "Only INR bilateral currency corridors are supported");
        }
    }

    private void validateAmount(String currency, BigDecimal amount) {

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Source amount must be greater than zero");
        }

        FxProperties.AmountLimit limit =
                properties.amountLimits().get(currency);

        Integer scale = properties.minorUnits().get(currency);

        if (limit == null || scale == null) {
            throw new IllegalStateException(
                    "Amount configuration is missing for " + currency);
        }

        if (amount.compareTo(limit.minimum()) < 0
                || amount.compareTo(limit.maximum()) > 0) {
            throw new IllegalArgumentException(
                    "Source amount is outside the permitted range");
        }

        if (amount.stripTrailingZeros().scale() > scale) {
            throw new IllegalArgumentException(
                    "Source amount exceeds the permitted currency precision");
        }
    }

    private FxQuoteResponse toResponse(FxQuote quote) {

        return new FxQuoteResponse(
                quote.getId(),
                quote.getSourceCurrency(),
                quote.getDestinationCurrency(),
                quote.getSourceAmount(),
                quote.getExchangeRate(),
                quote.getDestinationAmount(),
                quote.getProvider(),
                quote.getRateDate(),
                quote.getCreatedAt(),
                quote.getExpiresAt(),
                quote.isExpired(Instant.now()),
                quote.isConsumed()
        );
    }
}