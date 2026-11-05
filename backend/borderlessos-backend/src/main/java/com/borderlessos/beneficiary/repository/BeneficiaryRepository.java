package com.borderlessos.beneficiary.repository;

import com.borderlessos.beneficiary.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BeneficiaryRepository
        extends JpaRepository<Beneficiary, UUID> {

    // Retrieve all active beneficiaries belonging to a customer.
    List<Beneficiary> findAllByCustomer_IdAndActiveTrueOrderByCreatedAtDesc(
            UUID customerId
    );

    // Retrieve a specific active beneficiary belonging to a customer.
    Optional<Beneficiary> findByIdAndCustomer_IdAndActiveTrue(
            UUID beneficiaryId,
            UUID customerId
    );

    // Retrieve any beneficiary (active or inactive) belonging to a customer.
    Optional<Beneficiary> findByIdAndCustomer_Id(
            UUID beneficiaryId,
            UUID customerId
    );

    // Check whether a customer owns an active beneficiary.
    boolean existsByIdAndCustomer_IdAndActiveTrue(
            UUID beneficiaryId,
            UUID customerId
    );

    Optional<Beneficiary> findByIdAndCustomerIdAndActiveTrue(
            UUID id,
            UUID customerId
    );
}