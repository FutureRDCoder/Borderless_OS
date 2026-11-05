package com.borderlessos.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import org.springframework.http.HttpMethod;

import java.time.Instant;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository
    ) throws Exception {

        CookieCsrfTokenRepository csrfRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();

        CsrfTokenRequestAttributeHandler csrfHandler =
                new CsrfTokenRequestAttributeHandler();

        http
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository)
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId())
                )

                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(csrfHandler)
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/auth/csrf"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/login",
                                "/api/v1/auth/signup"
                        ).permitAll()

                        .requestMatchers(
                                "/actuator/health"
                        ).permitAll()

                        .anyRequest().authenticated()
                )

                .exceptionHandling(exception -> exception

                        .authenticationEntryPoint((request, response, authException) ->
                                writeSecurityError(
                                        request,
                                        response,
                                        401,
                                        "UNAUTHENTICATED",
                                        "Unauthorized",
                                        "Authentication required."
                                )
                        )

                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeSecurityError(
                                        request,
                                        response,
                                        403,
                                        "FORBIDDEN",
                                        "Forbidden",
                                        "You do not have permission to access this resource."
                                )
                        )
                )

                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(204)
                        )
                );

        return http.build();
    }

    private void writeSecurityError(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            int status,
            String code,
            String error,
            String message
    ) throws java.io.IOException {

        String requestId = request.getHeader("X-Request-Id");

        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        } else {
            requestId = requestId.trim();

            if (requestId.length() > 128) {
                requestId = requestId.substring(0, 128);
            }
        }

        response.setStatus(status);
        response.setContentType("application/json");
        response.setHeader("X-Request-Id", requestId);

        response.getWriter().write("""
                {"timestamp":"%s","status":%d,"code":"%s","error":"%s","message":"%s","path":"%s","requestId":"%s","validationErrors":{}}
                """.formatted(
                Instant.now(),
                status,
                code,
                error,
                message,
                request.getRequestURI(),
                requestId
        ).trim());
    }

}
