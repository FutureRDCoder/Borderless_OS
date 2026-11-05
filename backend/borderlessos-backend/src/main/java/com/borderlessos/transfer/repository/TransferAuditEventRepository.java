package com.borderlessos.transfer.repository;

import com.borderlessos.transfer.entity.TransferAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransferAuditEventRepository
        extends JpaRepository<TransferAuditEvent, UUID> {

    List<TransferAuditEvent> findAllByTransferIdOrderByCreatedAtAsc(
            UUID transferId
    );

    List<TransferAuditEvent> findTop200ByOrderByCreatedAtDesc();
}
