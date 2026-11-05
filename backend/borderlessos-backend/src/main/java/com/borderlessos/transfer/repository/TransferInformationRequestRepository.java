package com.borderlessos.transfer.repository;

import com.borderlessos.transfer.entity.TransferInformationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferInformationRequestRepository
        extends JpaRepository<TransferInformationRequest, UUID> {

    List<TransferInformationRequest>
    findAllByTransferIdOrderByCreatedAtAsc(UUID transferId);

    Optional<TransferInformationRequest>
    findFirstByTransferIdAndRespondedAtIsNullOrderByCreatedAtDesc(
            UUID transferId
    );
}
