package com.borderlessos.provider.repository;

import com.borderlessos.provider.entity.ProviderAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProviderAttemptRepository
        extends JpaRepository<ProviderAttempt, UUID> {

    Optional<ProviderAttempt> findByTransferId(UUID transferId);

    boolean existsByTransferId(UUID transferId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT a FROM ProviderAttempt a
    WHERE a.transferId = :transferId
    """)
    Optional<ProviderAttempt> findByTransferIdForUpdate(
            @Param("transferId") UUID transferId
    );
}
