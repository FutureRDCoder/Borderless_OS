package com.borderlessos.fx.repository;

import com.borderlessos.fx.entity.FxQuote;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FxQuoteRepository extends JpaRepository<FxQuote, UUID> {

    Optional<FxQuote> findByIdAndCustomerId(UUID id, UUID customerId);

    List<FxQuote> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT q FROM FxQuote q
    WHERE q.id = :quoteId
      AND q.customerId = :customerId
    """)
    Optional<FxQuote> findByIdAndCustomerIdForUpdate(
            @Param("quoteId") UUID quoteId,
            @Param("customerId") UUID customerId
    );
}