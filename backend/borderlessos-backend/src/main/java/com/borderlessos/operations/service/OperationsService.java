package com.borderlessos.operations.service;

import com.borderlessos.common.security.CurrentActor;
import com.borderlessos.common.security.CurrentActorService;
import com.borderlessos.compliance.entity.ComplianceHoldStatus;
import com.borderlessos.compliance.repository.ComplianceHoldRepository;
import com.borderlessos.operations.dto.response.AuditEventResponse;
import com.borderlessos.operations.dto.response.OperationsTransferResponse;
import com.borderlessos.provider.dto.response.ProviderAttemptResponse;
import com.borderlessos.provider.service.ProviderService;
import com.borderlessos.transfer.dto.response.InformationRequestResponse;
import com.borderlessos.transfer.entity.Transfer;
import com.borderlessos.transfer.entity.TransferInformationRequest;
import com.borderlessos.transfer.enums.TransferEventType;
import com.borderlessos.transfer.enums.TransferStatus;
import com.borderlessos.transfer.repository.TransferAuditEventRepository;
import com.borderlessos.transfer.repository.TransferInformationRequestRepository;
import com.borderlessos.transfer.repository.TransferRepository;
import com.borderlessos.transfer.service.TransferWorkflowService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OperationsService {

    private static final List<TransferStatus> DEFAULT_QUEUE_STATUSES = List.of(
            TransferStatus.SUBMITTED,
            TransferStatus.UNDER_REVIEW,
            TransferStatus.NEEDS_INFO,
            TransferStatus.READY_FOR_PROVIDER,
            TransferStatus.PROCESSING,
            TransferStatus.UNKNOWN
    );

    private final TransferRepository transferRepository;
    private final TransferAuditEventRepository auditRepository;
    private final TransferInformationRequestRepository informationRepository;
    private final ComplianceHoldRepository holdRepository;
    private final TransferWorkflowService workflowService;
    private final ProviderService providerService;
    private final CurrentActorService currentActorService;

    public OperationsService(
            TransferRepository transferRepository,
            TransferAuditEventRepository auditRepository,
            TransferInformationRequestRepository informationRepository,
            ComplianceHoldRepository holdRepository,
            TransferWorkflowService workflowService,
            ProviderService providerService,
            CurrentActorService currentActorService
    ) {
        this.transferRepository = transferRepository;
        this.auditRepository = auditRepository;
        this.informationRepository = informationRepository;
        this.holdRepository = holdRepository;
        this.workflowService = workflowService;
        this.providerService = providerService;
        this.currentActorService = currentActorService;
    }

    @Transactional(readOnly = true)
    public List<OperationsTransferResponse> getQueue(
            List<TransferStatus> statuses
    ) {
        List<TransferStatus> effectiveStatuses =
                statuses == null || statuses.isEmpty()
                        ? DEFAULT_QUEUE_STATUSES
                        : statuses;

        return transferRepository
                .findAllByStatusInOrderByCreatedAtAsc(effectiveStatuses)
                .stream()
                .map(OperationsTransferResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OperationsTransferResponse getTransferDetails(UUID transferId) {
        return OperationsTransferResponse.from(requireTransfer(transferId));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getDashboard() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("underReview", transferRepository.countByStatus(
                TransferStatus.UNDER_REVIEW
        ));
        counts.put("needsInfo", transferRepository.countByStatus(
                TransferStatus.NEEDS_INFO
        ));
        counts.put("readyForProvider", transferRepository.countByStatus(
                TransferStatus.READY_FOR_PROVIDER
        ));
        counts.put("processing", transferRepository.countByStatus(
                TransferStatus.PROCESSING
        ));
        counts.put("unknown", transferRepository.countByStatus(
                TransferStatus.UNKNOWN
        ));
        counts.put("complianceHold", transferRepository.countByStatus(
                TransferStatus.COMPLIANCE_HOLD
        ));
        return counts;
    }

    @Transactional
    public OperationsTransferResponse requestInformation(
            UUID transferId,
            String message
    ) {
        CurrentActor actor = requireOperationsActor();
        String validatedMessage = requireNonBlank(message, "Message");
        Instant now = Instant.now();

        Transfer transfer = lockTransfer(transferId);

        if (transfer.getStatus() != TransferStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "Information can only be requested while the transfer is under review."
            );
        }

        TransferInformationRequest informationRequest =
                TransferInformationRequest.create(
                        transferId,
                        actor,
                        validatedMessage,
                        now
                );
        informationRepository.save(informationRequest);

        workflowService.transition(
                transfer,
                TransferStatus.NEEDS_INFO,
                TransferEventType.INFORMATION_REQUESTED,
                actor,
                validatedMessage,
                validatedMessage,
                now
        );

        return OperationsTransferResponse.from(transfer);
    }

    @Transactional
    public OperationsTransferResponse reject(
            UUID transferId,
            String reason
    ) {
        CurrentActor actor = requireOperationsActor();
        String validatedReason = requireNonBlank(reason, "Reason");
        Instant now = Instant.now();

        Transfer transfer = lockTransfer(transferId);

        if (transfer.getStatus() != TransferStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "Only an UNDER_REVIEW transfer can be rejected."
            );
        }

        workflowService.transition(
                transfer,
                TransferStatus.REJECTED,
                TransferEventType.TRANSFER_REJECTED,
                actor,
                validatedReason,
                "Operations rejected the transfer.",
                now
        );

        return OperationsTransferResponse.from(transfer);
    }

    @Transactional
    public OperationsTransferResponse markReady(UUID transferId) {
        CurrentActor actor = requireOperationsActor();
        Instant now = Instant.now();

        Transfer transfer = lockTransfer(transferId);

        if (transfer.getStatus() != TransferStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "Only an UNDER_REVIEW transfer can be marked ready."
            );
        }

        if (holdRepository.existsByTransferIdAndStatus(
                transferId,
                ComplianceHoldStatus.ACTIVE
        )) {
            throw new IllegalStateException(
                    "A transfer with an active compliance hold cannot be marked ready."
            );
        }

        workflowService.transition(
                transfer,
                TransferStatus.READY_FOR_PROVIDER,
                TransferEventType.TRANSFER_APPROVED,
                actor,
                null,
                "Operations completed routine checks.",
                now
        );

        return OperationsTransferResponse.from(transfer);
    }

    @Transactional
    public ProviderAttemptResponse dispatch(UUID transferId) {
        return providerService.dispatch(transferId);
    }

    @Transactional
    public ProviderAttemptResponse inquireProvider(UUID transferId) {
        return providerService.inquire(transferId);
    }

    @Transactional(readOnly = true)
    public ProviderAttemptResponse getProviderAttempt(UUID transferId) {
        return providerService.getAttempt(transferId);
    }

    @Transactional(readOnly = true)
    public List<InformationRequestResponse> getInformationRequests(
            UUID transferId
    ) {
        requireTransfer(transferId);

        return informationRepository
                .findAllByTransferIdOrderByCreatedAtAsc(transferId)
                .stream()
                .map(InformationRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getAuditEvents(UUID transferId) {
        if (transferId != null) {
            requireTransfer(transferId);
            return auditRepository
                    .findAllByTransferIdOrderByCreatedAtAsc(transferId)
                    .stream()
                    .map(AuditEventResponse::forOperations)
                    .toList();
        }

        return auditRepository
                .findTop200ByOrderByCreatedAtDesc()
                .stream()
                .map(AuditEventResponse::forOperations)
                .toList();
    }

    private Transfer requireTransfer(UUID transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transfer not found."
                ));
    }

    private Transfer lockTransfer(UUID transferId) {
        return transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transfer not found."
                ));
    }

    private CurrentActor requireOperationsActor() {
        CurrentActor actor = currentActorService.requireActor();

        if (!actor.hasRole("OPERATIONS")) {
            throw new IllegalStateException(
                    "Only the OPERATIONS role may perform this action."
            );
        }

        return actor;
    }

    private String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        String trimmed = value.trim();

        if (trimmed.length() > 1000) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed 1000 characters."
            );
        }

        return trimmed;
    }
}
