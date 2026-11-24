package com.borderlessos.compliance.repository;

import com.borderlessos.compliance.entity.ComplianceHold;
import com.borderlessos.compliance.entity.ComplianceHoldStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplianceHoldRepository
        extends JpaRepository<ComplianceHold, UUID> {

    Optional<ComplianceHold> findFirstByTransferIdAndStatusOrderByOpenedAtDesc(
            UUID transferId,
            ComplianceHoldStatus status
    );

    Optional<ComplianceHold> findFirstByTransferIdOrderByOpenedAtDesc(
            UUID transferId
    );

    boolean existsByTransferIdAndStatus(
            UUID transferId,
            ComplianceHoldStatus status
    );

    boolean existsByTransferId(UUID transferId);

    List<ComplianceHold> findAllByStatusOrderByOpenedAtAsc(
            ComplianceHoldStatus status
    );

    long countByStatus(ComplianceHoldStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT h FROM ComplianceHold h
    WHERE h.transferId = :transferId
      AND h.status = com.borderlessos.compliance.entity.ComplianceHoldStatus.ACTIVE
    """)
    Optional<ComplianceHold> findActiveByTransferIdForUpdate(
            @Param("transferId") UUID transferId
    );
}
