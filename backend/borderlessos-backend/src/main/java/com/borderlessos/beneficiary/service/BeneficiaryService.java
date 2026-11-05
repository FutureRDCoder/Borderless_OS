package com.borderlessos.beneficiary.service;

import com.borderlessos.beneficiary.entity.Beneficiary;
import com.borderlessos.beneficiary.repository.BeneficiaryRepository;
import com.borderlessos.identity.entity.User;
import com.borderlessos.identity.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;

    // Create a beneficiary for an authenticated customer.
    @Transactional
    public Beneficiary createBeneficiary(
            UUID customerId,
            String recipientName,
            String destinationCountry,
            String destinationCurrency,
            String accountReference
    ) {

        User customer = findEnabledCustomer(customerId);

        String validatedRecipientName =
                validateText(recipientName, "Recipient name", 120);

        String validatedCountry =
                validateCode(destinationCountry, "Country", 2);

        String validatedCurrency =
                validateCode(destinationCurrency, "Currency", 3);

        String validatedAccountReference =
                validateText(accountReference, "Account reference", 64);

        Beneficiary beneficiary = new Beneficiary(
                customer,
                validatedRecipientName,
                validatedCountry,
                validatedCurrency,
                validatedAccountReference
        );

        return beneficiaryRepository.save(beneficiary);
    }

    // Retrieve all active beneficiaries belonging to a customer.
    @Transactional(readOnly = true)
    public List<Beneficiary> getMyBeneficiaries(UUID customerId) {

        findEnabledCustomer(customerId);

        return beneficiaryRepository
                .findAllByCustomer_IdAndActiveTrueOrderByCreatedAtDesc(
                        customerId
                );
    }

    // Retrieve one active beneficiary belonging to a customer.
    @Transactional(readOnly = true)
    public Beneficiary getMyBeneficiary(
            UUID customerId,
            UUID beneficiaryId
    ) {

        findEnabledCustomer(customerId);

        return beneficiaryRepository
                .findByIdAndCustomer_IdAndActiveTrue(
                        beneficiaryId,
                        customerId
                )
                .orElseThrow(() ->
                        new EntityNotFoundException("Beneficiary not found.")
                );
    }

    // Soft-deactivate a beneficiary belonging to a customer.
    @Transactional
    public void deactivateBeneficiary(
            UUID customerId,
            UUID beneficiaryId
    ) {

        findEnabledCustomer(customerId);

        Beneficiary beneficiary = beneficiaryRepository
                .findByIdAndCustomer_Id(beneficiaryId, customerId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Beneficiary not found.")
                );

        beneficiary.deactivate();
    }

    // Find and validate the customer.
    private User findEnabledCustomer(UUID customerId) {

        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is required.");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Customer not found.")
                );

        if (!customer.isEnabled()) {
            throw new IllegalStateException("Customer account is disabled.");
        }

        return customer;
    }

    // Validate and normalize text fields.
    private String validateText(
            String value,
            String fieldName,
            int maxLength
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        String normalizedValue = value.trim();

        if (normalizedValue.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed "
                            + maxLength + " characters."
            );
        }

        return normalizedValue;
    }

    // Validate and normalize country and currency codes.
    private String validateCode(
            String value,
            String fieldName,
            int expectedLength
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " code is required."
            );
        }

        String normalizedValue = value.trim()
                .toUpperCase(Locale.ROOT);

        if (normalizedValue.length() != expectedLength
                || !normalizedValue.matches("[A-Z]+")) {

            throw new IllegalArgumentException(
                    "Invalid " + fieldName + " code."
            );
        }

        return normalizedValue;
    }
}