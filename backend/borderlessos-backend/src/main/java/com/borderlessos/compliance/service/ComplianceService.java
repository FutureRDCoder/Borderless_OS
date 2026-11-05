package com.borderlessos.compliance.service;

import com.borderlessos.common.security.CurrentActor;
import com.borderlessos.common.security.CurrentActorService;
import com.borderlessos.compliance.config.ComplianceProperties;
import com.borderlessos.compliance.dto.response.ComplianceDecisionResponse;
import com.borderlessos.compliance.dto.response.ComplianceHoldResponse;
import com.borderlessos.compliance.entity.ComplianceDecision;
import com.borderlessos.compliance.entity.ComplianceDecisionType;
import com.borderlessos.compliance.entity.ComplianceHold;
import com.borderlessos.compliance.entity.ComplianceHoldStatus;
import com.borderlessos.compliance.repository.ComplianceDecisionRepository;
import com.borderlessos.compliance.repository.ComplianceHoldRepository;
import com.borderlessos.transfer.entity.Transfer;
import com.borderlessos.transfer.enums.TransferEventType;
import com.borderlessos.transfer.enums.TransferStatus;
import com.borderlessos.transfer.repository.TransferRepository;
import com.borderlessos.transfer.service.TransferWorkflowService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ComplianceService {

    private static final String SYSTEM_TRIGGER_TYPE = "DETERMINISTIC_SIMULATION";
    private static final String SAFE_HOLD_SUMMARY =
            "Transfer is under compliance review.";
    private static final String SAFE_CLEAR_SUMMARY =
            "Compliance review completed. Transfer returned to operations review.";

    private final ComplianceHoldRepository holdRepository;
    private final ComplianceDecisionRepository decisionRepository;
    private final TransferRepository transferRepository;
    private final TransferWorkflowService workflowService;
    private final ComplianceProperties properties;
    private final CurrentActorService currentActorService;

    public ComplianceService(
            ComplianceHoldRepository holdRepository,
            ComplianceDecisionRepository decisionRepository,
            TransferRepository transferRepository,
            TransferWorkflowService workflowService,
            ComplianceProperties properties,
            CurrentActorService currentActorService
    ) {
        this.holdRepository = holdRepository;
        this.decisionRepository = decisionRepository;
        this.transferRepository = transferRepository;
        this.workflowService = workflowService;
        this.properties = properties;
        this.currentActorService = currentActorService;
    }

    @Transactional
    public void evaluateSubmissionTrigger(Transfer transfer, Instant now) {
        if (transfer.getStatus() != TransferStatus.UNDER_REVIEW) {
            return;
        }

        String country = transfer.getDestinationCountry() == null
                ? ""
                : transfer.getDestinationCountry().toUpperCase(Locale.ROOT);

        boolean shouldHold = properties.holdCountries() != null
                && properties.holdCountries()
                .stream()
                .map(value -> value.toUpperCase(Locale.ROOT))
                .anyMatch(country::equals);

        if (!shouldHold || holdRepository.existsByTransferId(
                transfer.getId()
        )) {
            return;
        }

        ComplianceHold hold = ComplianceHold.place(
                transfer.getId(),
                SYSTEM_TRIGGER_TYPE,
                properties.ruleId(),
                "Simulated rule " + properties.ruleId()
                        + " requires compliance review for destination "
                        + country + ".",
                CurrentActorService.SYSTEM,
                now
        );

        holdRepository.save(hold);

        decisionRepository.save(ComplianceDecision.create(
                transfer.getId(),
                hold.getId(),
                ComplianceDecisionType.HOLD_TRIGGERED,
                CurrentActorService.SYSTEM,
                null,
                now
        ));

        workflowService.transition(
                transfer,
                TransferStatus.COMPLIANCE_HOLD,
                TransferEventType.COMPLIANCE_HOLD_PLACED,
                CurrentActorService.SYSTEM,
                null,
                SAFE_HOLD_SUMMARY,
                now
        );
    }

    @Transactional
    public ComplianceHoldResponse placeHold(UUID transferId, String reason) {
        CurrentActor actor = requireComplianceActor();
        String validatedReason = requireReason(reason);
        Instant now = Instant.now();

        Transfer transfer = transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transfer not found."
                ));

        if (transfer.getStatus() != TransferStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "A hold can only be placed while the transfer is under review."
            );
        }

        if (holdRepository.existsByTransferIdAndStatus(
                transferId,
                ComplianceHoldStatus.ACTIVE
        )) {
            throw new IllegalStateException(
                    "The transfer already has an active compliance hold."
            );
        }

        ComplianceHold hold = ComplianceHold.place(
                transferId,
                "MANUAL_COMPLIANCE_REVIEW",
                properties.ruleId(),
                "Compliance placed a manual review hold.",
                actor,
                now
        );
        holdRepository.save(hold);

        decisionRepository.save(ComplianceDecision.create(
                transferId,
                hold.getId(),
                ComplianceDecisionType.HOLD_REPLACED,
                actor,
                validatedReason,
                now
        ));

        workflowService.transition(
                transfer,
                TransferStatus.COMPLIANCE_HOLD,
                TransferEventType.COMPLIANCE_HOLD_PLACED,
                actor,
                validatedReason,
                SAFE_HOLD_SUMMARY,
                now
        );

        return ComplianceHoldResponse.from(hold, transfer);
    }

    @Transactional
    public ComplianceHoldResponse maintainHold(
            UUID transferId,
            String reason
    ) {
        CurrentActor actor = requireComplianceActor();
        String validatedReason = requireReason(reason);
        Instant now = Instant.now();

        Transfer transfer = transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transfer not found."
                ));

        if (transfer.getStatus() != TransferStatus.COMPLIANCE_HOLD) {
            throw new IllegalStateException(
                    "The transfer is not on compliance hold."
            );
        }

        ComplianceHold hold = holdRepository
                .findActiveByTransferIdForUpdate(transferId)
                .orElseThrow(() -> new IllegalStateException(
                        "No active compliance hold was found."
                ));

        decisionRepository.save(ComplianceDecision.create(
                transferId,
                hold.getId(),
                ComplianceDecisionType.HOLD_MAINTAINED,
                actor,
                validatedReason,
                now
        ));

        workflowService.record(
                transfer,
                TransferEventType.COMPLIANCE_HOLD_MAINTAINED,
                actor,
                validatedReason,
                "Compliance hold maintained.",
                now
        );

        return ComplianceHoldResponse.from(hold, transfer);
    }

    @Transactional
    public ComplianceHoldResponse clearHold(UUID transferId, String reason) {
        CurrentActor actor = requireComplianceActor();
        String validatedReason = requireReason(reason);
        Instant now = Instant.now();

        Transfer transfer = transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transfer not found."
                ));

        if (transfer.getStatus() != TransferStatus.COMPLIANCE_HOLD) {
            throw new IllegalStateException(
                    "Only a transfer on compliance hold can be cleared."
            );
        }

        ComplianceHold hold = holdRepository
                .findActiveByTransferIdForUpdate(transferId)
                .orElseThrow(() -> new IllegalStateException(
                        "No active compliance hold was found."
                ));

        hold.clear(actor, validatedReason, now);

        decisionRepository.save(ComplianceDecision.create(
                transferId,
                hold.getId(),
                ComplianceDecisionType.HOLD_CLEARED,
                actor,
                validatedReason,
                now
        ));

        workflowService.transition(
                transfer,
                TransferStatus.UNDER_REVIEW,
                TransferEventType.COMPLIANCE_HOLD_CLEARED,
                actor,
                validatedReason,
                SAFE_CLEAR_SUMMARY,
                now
        );

        return ComplianceHoldResponse.from(hold, transfer);
    }

    @Transactional(readOnly = true)
    public List<ComplianceHoldResponse> getActiveHolds() {
        return holdRepository
                .findAllByStatusOrderByOpenedAtAsc(
                        ComplianceHoldStatus.ACTIVE
                )
                .stream()
                .map(hold -> ComplianceHoldResponse.from(
                        hold,
                        requireTransfer(hold.getTransferId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ComplianceHoldResponse getHold(UUID transferId) {
        ComplianceHold hold = holdRepository
                .findFirstByTransferIdAndStatusOrderByOpenedAtDesc(
                        transferId,
                        ComplianceHoldStatus.ACTIVE
                )
                .orElseThrow(() -> new EntityNotFoundException(
                        "Active compliance hold not found."
                ));

        return ComplianceHoldResponse.from(hold, requireTransfer(transferId));
    }

    @Transactional(readOnly = true)
    public List<ComplianceDecisionResponse> getDecisions(UUID transferId) {
        return decisionRepository
                .findAllByTransferIdOrderByCreatedAtAsc(transferId)
                .stream()
                .map(ComplianceDecisionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComplianceDecisionResponse> getRecentDecisions() {
        return decisionRepository
                .findTop200ByOrderByCreatedAtDesc()
                .stream()
                .map(ComplianceDecisionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countActiveHolds() {
        return holdRepository.countByStatus(ComplianceHoldStatus.ACTIVE);
    }

    private Transfer requireTransfer(UUID transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transfer not found."
                ));
    }

    private CurrentActor requireComplianceActor() {
        CurrentActor actor = currentActorService.requireActor();

        if (!actor.hasRole("COMPLIANCE")) {
            throw new IllegalStateException(
                    "Only the COMPLIANCE role may perform this action."
            );
        }

        return actor;
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required.");
        }

        String trimmed = reason.trim();
        if (trimmed.length() > 1000) {
            throw new IllegalArgumentException(
                    "Reason must not exceed 1000 characters."
            );
        }

        return trimmed;
    }
}
