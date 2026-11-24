package com.borderlessos.transfer.repository;

import com.borderlessos.transfer.entity.Transfer;
import com.borderlessos.transfer.enums.TransferStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    Optional<Transfer> findByIdAndCustomerId(UUID id, UUID customerId);

    List<Transfer> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<Transfer> findAllByStatusInOrderByCreatedAtAsc(
            Collection<TransferStatus> statuses
    );

    List<Transfer> findTop200ByStatusOrderByCreatedAtAsc(TransferStatus status);

    List<Transfer> findAllByOrderByCreatedAtDesc();

    List<Transfer> findTop200ByOrderByCreatedAtDesc();

    long countByStatus(TransferStatus status);

    long countByStatusIn(Collection<TransferStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transfer t WHERE t.id = :id")
    Optional<Transfer> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT t FROM Transfer t
    WHERE t.id = :id
      AND t.customerId = :customerId
    """)
    Optional<Transfer> findByIdAndCustomerIdForUpdate(
            @Param("id") UUID id,
            @Param("customerId") UUID customerId
    );
}
