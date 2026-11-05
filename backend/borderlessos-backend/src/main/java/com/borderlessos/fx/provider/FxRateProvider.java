package com.borderlessos.fx.provider;

public interface FxRateProvider {

    FxRate fetchRate(
            String sourceCurrency,
            String destinationCurrency
    );
}