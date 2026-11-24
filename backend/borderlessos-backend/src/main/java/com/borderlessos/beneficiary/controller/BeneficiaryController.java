
package com.borderlessos.beneficiary.controller;

import com.borderlessos.beneficiary.dto.request.BeneficiaryCreateRequest;
import com.borderlessos.beneficiary.dto.response.BeneficiaryResponse;
import com.borderlessos.beneficiary.entity.Beneficiary;
import com.borderlessos.beneficiary.service.BeneficiaryService;
import com.borderlessos.identity.entity.User;
import com.borderlessos.identity.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/beneficiaries")
@RequiredArgsConstructor
@PreAuthorize("hasRole(@appRoles.customer())")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;
    private final UserRepository userRepository;

    // Create a new beneficiary.
    @PostMapping
    public ResponseEntity<BeneficiaryResponse> createBeneficiary(
            @Valid @RequestBody BeneficiaryCreateRequest request,
            Authentication authentication
    ) {

        UUID customerId = getAuthenticatedCustomerId(authentication);

        Beneficiary beneficiary = beneficiaryService.createBeneficiary(
                customerId,
                request.recipientName(),
                request.destinationCountry(),
                request.destinationCurrency(),
                request.accountReference()
        );

        BeneficiaryResponse response =
                BeneficiaryResponse.from(beneficiary);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    // Retrieve all active beneficiaries belonging to the customer.
    @GetMapping
    public ResponseEntity<List<BeneficiaryResponse>> getMyBeneficiaries(
            Authentication authentication
    ) {

        UUID customerId = getAuthenticatedCustomerId(authentication);

        List<BeneficiaryResponse> responses = beneficiaryService
                .getMyBeneficiaries(customerId)
                .stream()
                .map(BeneficiaryResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    // Retrieve one active beneficiary belonging to the customer.
    @GetMapping("/{id}")
    public ResponseEntity<BeneficiaryResponse> getMyBeneficiary(
            @PathVariable UUID id,
            Authentication authentication
    ) {

        UUID customerId = getAuthenticatedCustomerId(authentication);

        Beneficiary beneficiary = beneficiaryService.getMyBeneficiary(
                customerId,
                id
        );

        return ResponseEntity.ok(
                BeneficiaryResponse.from(beneficiary)
        );
    }

    // Soft-deactivate a beneficiary.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateBeneficiary(
            @PathVariable UUID id,
            Authentication authentication
    ) {

        UUID customerId = getAuthenticatedCustomerId(authentication);

        beneficiaryService.deactivateBeneficiary(customerId, id);

        return ResponseEntity.noContent().build();
    }

    // Resolve the authenticated email to its database UUID.
    private UUID getAuthenticatedCustomerId(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentication is required.");
        }

        User customer = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new EntityNotFoundException("Authenticated user not found.")
                );

        return customer.getId();
    }
}
