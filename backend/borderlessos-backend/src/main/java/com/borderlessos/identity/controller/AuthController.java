package com.borderlessos.identity.controller;

import com.borderlessos.common.dto.response.ApiErrorResponse;
import com.borderlessos.identity.dto.response.AuthUserResponse;
import com.borderlessos.identity.dto.request.LoginRequest;
import com.borderlessos.identity.dto.request.SignupRequest;
import com.borderlessos.identity.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;

import org.springframework.security.web.csrf.CsrfToken;

import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserService userService;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            UserService userService
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.userService = userService;
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(
                new CsrfResponse(csrfToken.getToken())
        );
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {

        if (request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()) {
            return buildError(
                    httpRequest,
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Email and password are required.",
                    Map.of()
            );
        }

        try {
            Authentication authentication = authenticateAndLogin(
                    request.email(),
                    request.password(),
                    httpRequest,
                    httpResponse
            );

            return ResponseEntity.ok(toResponse(authentication));

        } catch (AuthenticationException exception) {

            SecurityContextHolder.clearContext();

            return buildError(
                    httpRequest,
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHENTICATED",
                    "Invalid email or password",
                    Map.of()
            );
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {

        userService.createUser(
                request.email(),
                request.password(),
                request.displayName(),
                "CUSTOMER"
        );

        Authentication authentication = authenticateAndLogin(
                request.email(),
                request.password(),
                httpRequest,
                httpResponse
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(authentication));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> me(
            Authentication authentication
    ) {
        return ResponseEntity.ok(toResponse(authentication));
    }

    private Authentication authenticateAndLogin(
            String email,
            String password,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                email.trim().toLowerCase(Locale.ROOT),
                                password
                        )
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(
                context,
                httpRequest,
                httpResponse
        );

        return authentication;
    }

    private AuthUserResponse toResponse(Authentication authentication) {

        Set<String> roles = authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());

        return new AuthUserResponse(
                authentication.getName(),
                roles
        );
    }

    private ResponseEntity<ApiErrorResponse> buildError(
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String message,
            Map<String, String> validationErrors
    ) {

        String requestId = request.getHeader("X-Request-Id");

        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        } else {
            requestId = requestId.trim();

            if (requestId.length() > 128) {
                requestId = requestId.substring(0, 128);
            }
        }

        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                code,
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                requestId,
                validationErrors
        );

        return ResponseEntity.status(status)
                .header("X-Request-Id", requestId)
                .body(response);
    }

    public record CsrfResponse(String token) {
    }
}