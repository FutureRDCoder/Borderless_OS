package com.borderlessos.fx.controller;

import com.borderlessos.fx.dto.request.FxQuoteRequest;
import com.borderlessos.fx.dto.response.FxQuoteResponse;
import com.borderlessos.fx.service.FxQuoteService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fx/quotes")
@PreAuthorize("hasRole('CUSTOMER')")
public class FxQuoteController {

    private final FxQuoteService fxQuoteService;

    public FxQuoteController(FxQuoteService fxQuoteService) {
        this.fxQuoteService = fxQuoteService;
    }

    @PostMapping
    public ResponseEntity<FxQuoteResponse> createQuote(
            @Valid @RequestBody FxQuoteRequest request
    ) {
        FxQuoteResponse response = fxQuoteService.createQuote(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FxQuoteResponse> getQuote(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(fxQuoteService.getQuote(id));
    }
}