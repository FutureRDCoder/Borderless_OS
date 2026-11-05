package com.borderlessos.compliance.repository;

import com.borderlessos.compliance.entity.ComplianceDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplianceDecisionRepository
        extends JpaRepository<ComplianceDecision, UUID> {

    List<ComplianceDecision> findAllByTransferIdOrderByCreatedAtAsc(
            UUID transferId
    );

    List<ComplianceDecision> findTop200ByOrderByCreatedAtDesc();
}
