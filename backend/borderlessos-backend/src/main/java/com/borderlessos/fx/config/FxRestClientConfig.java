package com.borderlessos.fx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class FxRestClientConfig {

    @Bean
    public RestClient fxRestClient(FxProperties properties) {

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(
                properties.frankfurter().connectTimeout()
        );

        requestFactory.setReadTimeout(
                properties.frankfurter().readTimeout()
        );

        return RestClient.builder()
                .baseUrl(properties.frankfurter().baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}