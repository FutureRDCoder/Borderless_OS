package com.borderlessos.provider.service;

import com.borderlessos.common.security.CurrentActor;
import com.borderlessos.common.security.CurrentActorService;
import com.borderlessos.compliance.entity.ComplianceHoldStatus;
import com.borderlessos.compliance.repository.ComplianceHoldRepository;
import com.borderlessos.idempotency.RequestFingerprint;
import com.borderlessos.provider.config.ProviderProperties;
import com.borderlessos.provider.dto.response.ProviderAttemptResponse;
import com.borderlessos.provider.entity.ProviderAttempt;
import com.borderlessos.provider.entity.ProviderInquiry;
import com.borderlessos.provider.entity.ProviderInquiryResult;
import com.borderlessos.provider.entity.ProviderOutcome;
import com.borderlessos.provider.repository.ProviderAttemptRepository;
import com.borderlessos.provider.repository.ProviderInquiryRepository;
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
import java.util.UUID;

@Service
public class ProviderService {

    private final ProviderAttemptRepository attemptRepository;
    private final ProviderInquiryRepository inquiryRepository;
    private final TransferRepository transferRepository;
    private final ComplianceHoldRepository holdRepository;
    private final TransferWorkflowService workflowService;
    private final MockProviderClient mockProviderClient;
    private final ProviderProperties properties;
    private final CurrentActorService currentActorService;

    public ProviderService(
            ProviderAttemptRepository attemptRepository,
            ProviderInquiryRepository inquiryRepository,
            TransferRepository transferRepository,
            ComplianceHoldRepository holdRepository,
            TransferWorkflowService workflowService,
            MockProviderClient mockProviderClient,
            ProviderProperties properties,
            CurrentActorService currentActorService
    ) {
        this.attemptRepository = attemptRepository;
        this.inquiryRepository = inquiryRepository;
        this.transferRepository = transferRepository;
        this.holdRepository = holdRepository;
        this.workflowService = workflowService;
        this.mockProviderClient = mockProviderClient;
        this.properties = properties;
        this.currentActorService = currentActorService;
    }

    @Transactional
    public ProviderAttemptResponse dispatch(UUID transferId) {
        CurrentActor actor = requireOperationsActor();

        Transfer transfer = transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transfer not found."
                ));

        ProviderAttempt existingAttempt = attemptRepository
                .findByTransferIdForUpdate(transferId)
                .orElse(null);

        if (existingAttempt != null) {
            return ProviderAttemptResponse.from(
                    existingAttempt,
                    inquiryRepository
                            .findAllByTransferIdOrderByCreatedAtAsc(transferId)
            );
        }

        if (transfer.getStatus() != TransferStatus.READY_FOR_PROVIDER) {
            throw new IllegalStateException(
                    "Only a READY_FOR_PROVIDER transfer can be dispatched."
            );
        }

        if (holdRepository.existsByTransferIdAndStatus(
                transferId,
                ComplianceHoldStatus.ACTIVE
        )) {
            throw new IllegalStateException(
                    "A transfer with an active compliance hold cannot be dispatched."
            );
        }

        Instant now = Instant.now();
        ProviderOutcome scenario = mockProviderClient.determineScenario(
                transfer.getDestinationCountry()
        );

        ProviderAttempt attempt = ProviderAttempt.start(
                transferId,
                properties.name(),
                mockProviderClient.createProviderReference(transferId),
                "DISPATCH-" + transferId,
                RequestFingerprint.sha256(String.join("|",
                        transferId.toString(),
                        transfer.getDestinationCountry(),
                        scenario.name()
                )),
                scenario,
                now
        );

        attemptRepository.save(attempt);

        workflowService.transition(
                transfer,
                TransferStatus.PROCESSING,
                TransferEventType.PROVIDER_ATTEMPT_STARTED,
                actor,
                null,
                "Mock provider attempt started.",
                now
        );

        switch (scenario) {
            case SUCCESS -> {
                attempt.markCompleted(now);
                workflowService.transition(
                        transfer,
                        TransferStatus.SUCCEEDED,
                        TransferEventType.PROVIDER_STATUS_CONFIRMED,
                        CurrentActorService.SYSTEM,
                        null,
                        "Mock provider confirmed success.",
                        now
                );
            }
            case FAILURE -> {
                attempt.markFailed(now);
                workflowService.transition(
                        transfer,
                        TransferStatus.FAILED,
                        TransferEventType.PROVIDER_STATUS_CONFIRMED,
                        CurrentActorService.SYSTEM,
                        null,
                        "Mock provider confirmed failure.",
                        now
                );
            }
            case TIMEOUT_RESOLVES_SUCCESS -> {
                attempt.markUnknown(ProviderOutcome.SUCCESS, now);
                workflowService.transition(
                        transfer,
                        TransferStatus.UNKNOWN,
                        TransferEventType.PROVIDER_STATUS_UNKNOWN,
                        CurrentActorService.SYSTEM,
                        null,
                        "Provider outcome is unknown; a status inquiry is required.",
                        now
                );
            }
            case TIMEOUT_INCONCLUSIVE -> {
                attempt.markUnknown(null, now);
                workflowService.transition(
                        transfer,
                        TransferStatus.UNKNOWN,
                        TransferEventType.PROVIDER_STATUS_UNKNOWN,
                        CurrentActorService.SYSTEM,
                        null,
                        "Provider outcome is unknown; a status inquiry is required.",
                        now
                );
            }
        }

        return ProviderAttemptResponse.from(
                attempt,
                List.of()
        );
    }

    @Transactional
    public ProviderAttemptResponse inquire(UUID transferId) {
        CurrentActor actor = requireOperationsActor();
        Instant now = Instant.now();

        Transfer transfer = transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transfer not found."
                ));

        if (transfer.getStatus() != TransferStatus.UNKNOWN) {
            throw new IllegalStateException(
                    "A provider inquiry is only permitted while the outcome is UNKNOWN."
            );
        }

        ProviderAttempt attempt = attemptRepository
                .findByTransferIdForUpdate(transferId)
                .orElseThrow(() -> new IllegalStateException(
                        "No provider attempt was found for this transfer."
                ));

        ProviderInquiryResult result;
        ProviderOutcome resolvedOutcome = null;

        if (attempt.getScenario() == ProviderOutcome.TIMEOUT_RESOLVES_SUCCESS
                && attempt.getPendingOutcome() != null) {
            resolvedOutcome = attempt.getPendingOutcome();
            result = resolvedOutcome == ProviderOutcome.SUCCESS
                    ? ProviderInquiryResult.SUCCESS
                    : ProviderInquiryResult.FAILURE;

            attempt.resolvePending(resolvedOutcome, now);

            TransferStatus nextStatus = resolvedOutcome == ProviderOutcome.SUCCESS
                    ? TransferStatus.SUCCEEDED
                    : TransferStatus.FAILED;

            workflowService.transition(
                    transfer,
                    nextStatus,
                    TransferEventType.PROVIDER_STATUS_CONFIRMED,
                    actor,
                    null,
                    "Provider status inquiry resolved the outcome.",
                    now
            );
        } else {
            result = ProviderInquiryResult.INCONCLUSIVE;
            attempt.recordInquiry(now);

            workflowService.record(
                    transfer,
                    TransferEventType.PROVIDER_INQUIRY_INCONCLUSIVE,
                    actor,
                    null,
                    "Provider status inquiry remained inconclusive.",
                    now
            );
        }

        ProviderInquiry inquiry = ProviderInquiry.create(
                attempt.getId(),
                transferId,
                actor,
                result,
                resolvedOutcome,
                now
        );

        inquiryRepository.save(inquiry);

        return ProviderAttemptResponse.from(
                attempt,
                inquiryRepository
                        .findAllByTransferIdOrderByCreatedAtAsc(transferId)
        );
    }

    @Transactional(readOnly = true)
    public ProviderAttemptResponse getAttempt(UUID transferId) {
        ProviderAttempt attempt = attemptRepository.findByTransferId(transferId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Provider attempt not found."
                ));

        return ProviderAttemptResponse.from(
                attempt,
                inquiryRepository
                        .findAllByTransferIdOrderByCreatedAtAsc(transferId)
        );
    }

    private CurrentActor requireOperationsActor() {
        CurrentActor actor = currentActorService.requireActor();

        if (!actor.hasRole("OPERATIONS")) {
            throw new IllegalStateException(
                    "Only the OPERATIONS role may perform provider actions."
            );
        }

        return actor;
    }
}
