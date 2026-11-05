package com.borderlessos.idempotency.repository;

import com.borderlessos.idempotency.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository
        extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByCustomerIdAndOperationAndIdempotencyKey(
            UUID customerId,
            String operation,
            String idempotencyKey
    );
}
