package com.borderlessos.transfer.service;

import com.borderlessos.beneficiary.entity.Beneficiary;
import com.borderlessos.beneficiary.repository.BeneficiaryRepository;
import com.borderlessos.common.security.AppRoles;
import com.borderlessos.common.security.CurrentActor;
import com.borderlessos.common.security.CurrentActorService;
import com.borderlessos.compliance.service.ComplianceService;
import com.borderlessos.fx.config.FxProperties;
import com.borderlessos.fx.entity.FxQuote;
import com.borderlessos.fx.repository.FxQuoteRepository;
import com.borderlessos.idempotency.RequestFingerprint;
import com.borderlessos.idempotency.entity.IdempotencyRecord;
import com.borderlessos.idempotency.repository.IdempotencyRecordRepository;
import com.borderlessos.transfer.dto.request.CreateTransferRequest;
import com.borderlessos.transfer.dto.response.CustomerTimelineEventResponse;
import com.borderlessos.transfer.dto.response.TransferResponse;
import com.borderlessos.transfer.entity.Transfer;
import com.borderlessos.transfer.entity.TransferAuditEvent;
import com.borderlessos.transfer.entity.TransferInformationRequest;
import com.borderlessos.transfer.enums.TransferEventType;
import com.borderlessos.transfer.enums.TransferStatus;
import com.borderlessos.transfer.repository.TransferAuditEventRepository;
import com.borderlessos.transfer.repository.TransferInformationRequestRepository;
import com.borderlessos.transfer.repository.TransferRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TransferService {

    private static final Duration IDEMPOTENCY_RETENTION = Duration.ofHours(24);
    private static final String SUBMIT_OPERATION = "TRANSFER_SUBMIT";

    private final BeneficiaryRepository beneficiaryRepository;
    private final FxQuoteRepository fxQuoteRepository;
    private final TransferRepository transferRepository;
    private final TransferAuditEventRepository auditRepository;
    private final TransferInformationRequestRepository informationRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final TransferWorkflowService workflowService;
    private final ComplianceService complianceService;
    private final CurrentActorService currentActorService;
    private final FxProperties fxProperties;

    public TransferService(
            BeneficiaryRepository beneficiaryRepository,
            FxQuoteRepository fxQuoteRepository,
            TransferRepository transferRepository,
            TransferAuditEventRepository auditRepository,
            TransferInformationRequestRepository informationRepository,
            IdempotencyRecordRepository idempotencyRepository,
            TransferWorkflowService workflowService,
            ComplianceService complianceService,
            CurrentActorService currentActorService,
            FxProperties fxProperties
    ) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.fxQuoteRepository = fxQuoteRepository;
        this.transferRepository = transferRepository;
        this.auditRepository = auditRepository;
        this.informationRepository = informationRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.workflowService = workflowService;
        this.complianceService = complianceService;
        this.currentActorService = currentActorService;
        this.fxProperties = fxProperties;
    }

    @Transactional
    public TransferResponse createDraft(CreateTransferRequest request) {
        CurrentActor actor = requireCustomer();
        Instant now = Instant.now();

        Beneficiary beneficiary = findActiveBeneficiary(
                request.beneficiaryId(),
                actor.id()
        );

        FxQuote quote = findOwnedQuote(request.quoteId(), actor.id());

        if (quote.isConsumed()) {
            throw new IllegalStateException(
                    "FX quote has already been used."
            );
        }

        validateQuoteMatchesBeneficiary(quote, beneficiary);

        Transfer transfer = Transfer.draft(
                actor.id(),
                beneficiary.getId(),
                quote.getId(),
                quoteSource(quote),
                beneficiary.getRecipientName(),
                beneficiary.getDestinationCountry(),
                beneficiary.getDestinationCurrency(),
                beneficiary.getAccountReference(),
                quote.getSourceCurrency(),
                quote.getSourceAmount(),
                quote.getDestinationAmount(),
                quote.getExchangeRate(),
                now
        );

        Transfer savedTransfer = transferRepository.save(transfer);

        auditRepository.save(TransferAuditEvent.create(
                savedTransfer.getId(),
                actor.id(),
                actor.email(),
                actor.role(),
                TransferEventType.TRANSFER_CREATED,
                null,
                TransferStatus.DRAFT,
                null,
                "Draft transfer created.",
                now
        ));

        return TransferResponse.from(savedTransfer);
    }

    @Transactional
    public TransferResponse updateDraft(
            UUID transferId,
            CreateTransferRequest request
    ) {
        CurrentActor actor = requireCustomer();
        Instant now = Instant.now();

        Transfer transfer = transferRepository
                .findByIdAndCustomerIdForUpdate(transferId, actor.id())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transfer not found."
                ));

        if (transfer.getStatus() != TransferStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only a DRAFT transfer can be edited."
            );
        }

        Beneficiary beneficiary = findActiveBeneficiary(
                request.beneficiaryId(),
                actor.id()
        );

        FxQuote quote = findOwnedQuote(request.quoteId(), actor.id());

        if (quote.isConsumed()) {
            throw new IllegalStateException(
                    "FX quote has already been used."
            );
        }

        validateQuoteMatchesBeneficiary(quote, beneficiary);

        transfer.refreshDraft(
                beneficiary.getId(),
                quote.getId(),
                quoteSource(quote),
                beneficiary.getRecipientName(),
                beneficiary.getDestinationCountry(),
                beneficiary.getDestinationCurrency(),
                beneficiary.getAccountReference(),
                quote.getSourceCurrency(),
                quote.getSourceAmount(),
                quote.getDestinationAmount(),
                quote.getExchangeRate(),
                now
        );

        return TransferResponse.from(transfer);
    }

    @Transactional
    public TransferResponse submit(UUID transferId, String idempotencyKey) {
        CurrentActor actor = requireCustomer();
        String validatedKey = validateIdempotencyKey(idempotencyKey);

        Transfer transfer = transferRepository
                .findByIdAndCustomerIdForUpdate(transferId, actor.id())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transfer not found."
                ));

        String fingerprint = submissionFingerprint(transfer);
        Instant now = Instant.now();

        IdempotencyRecord existingRecord = idempotencyRepository
                .findByCustomerIdAndOperationAndIdempotencyKey(
                        actor.id(),
                        SUBMIT_OPERATION,
                        validatedKey
                )
                .orElse(null);

        if (existingRecord != null) {
            if (existingRecord.isExpired(now)) {
                idempotencyRepository.delete(existingRecord);
            } else if (!existingRecord.getRequestFingerprint()
                    .equals(fingerprint)) {
                throw new IllegalStateException(
                        "Idempotency key was reused with different request data."
                );
            } else {
                return TransferResponse.from(transfer);
            }
        }

        if (transfer.getStatus() != TransferStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only a DRAFT transfer can be submitted."
            );
        }

        Beneficiary beneficiary = findActiveBeneficiary(
                transfer.getBeneficiaryId(),
                actor.id()
        );

        FxQuote quote = fxQuoteRepository
                .findByIdAndCustomerIdForUpdate(
                        transfer.getFxQuoteId(),
                        actor.id()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "FX quote not found."
                ));

        validateQuoteMatchesBeneficiary(quote, beneficiary);
        validateQuoteMatchesSnapshot(quote, transfer);
        validateAmount(quote.getSourceCurrency(), quote.getSourceAmount());

        if (quote.isConsumed()) {
            throw new IllegalStateException(
                    "FX quote has already been used."
            );
        }

        quote.consume(now);
        transfer.markSubmitted(now);

        workflowService.transition(
                transfer,
                TransferStatus.SUBMITTED,
                TransferEventType.TRANSFER_SUBMITTED,
                actor,
                null,
                "Customer submitted the transfer for review.",
                now
        );

        workflowService.transition(
                transfer,
                TransferStatus.UNDER_REVIEW,
                TransferEventType.REVIEW_STARTED,
                CurrentActorService.SYSTEM,
                null,
                "Transfer accepted into operations review.",
                now
        );

        complianceService.evaluateSubmissionTrigger(transfer, now);

        idempotencyRepository.save(IdempotencyRecord.create(
                actor.id(),
                SUBMIT_OPERATION,
                validatedKey,
                fingerprint,
                transfer.getId(),
                now,
                now.plus(IDEMPOTENCY_RETENTION)
        ));

        return TransferResponse.from(transfer);
    }

    @Transactional
    public TransferResponse respondToInformationRequest(
            UUID transferId,
            String message
    ) {
        CurrentActor actor = requireCustomer();
        String validatedMessage = validateMessage(message);
        Instant now = Instant.now();

        Transfer transfer = transferRepository
                .findByIdAndCustomerIdForUpdate(transferId, actor.id())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transfer not found."
                ));

        if (transfer.getStatus() != TransferStatus.NEEDS_INFO) {
            throw new IllegalStateException(
                    "The transfer is not awaiting customer information."
            );
        }

        TransferInformationRequest informationRequest = informationRepository
                .findFirstByTransferIdAndRespondedAtIsNullOrderByCreatedAtDesc(
                        transferId
                )
                .orElseThrow(() -> new IllegalStateException(
                        "No open information request was found."
                ));

        informationRequest.respond(actor, validatedMessage, now);

        workflowService.transition(
                transfer,
                TransferStatus.SUBMITTED,
                TransferEventType.INFORMATION_SUBMITTED,
                actor,
                validatedMessage,
                "Customer supplied the requested information.",
                now
        );

        workflowService.transition(
                transfer,
                TransferStatus.UNDER_REVIEW,
                TransferEventType.REVIEW_STARTED,
                CurrentActorService.SYSTEM,
                null,
                "Transfer returned to operations review.",
                now
        );

        complianceService.evaluateSubmissionTrigger(transfer, now);

        return TransferResponse.from(transfer);
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> getMyTransfers() {
        CurrentActor actor = requireCustomer();

        return transferRepository
                .findAllByCustomerIdOrderByCreatedAtDesc(actor.id())
                .stream()
                .map(TransferResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransferResponse getMyTransfer(UUID transferId) {
        CurrentActor actor = requireCustomer();

        Transfer transfer = transferRepository
                .findByIdAndCustomerId(transferId, actor.id())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transfer not found."
                ));

        return TransferResponse.from(transfer);
    }

    @Transactional(readOnly = true)
    public List<CustomerTimelineEventResponse> getMyTransferTimeline(
            UUID transferId
    ) {
        CurrentActor actor = requireCustomer();

        transferRepository
                .findByIdAndCustomerId(transferId, actor.id())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transfer not found."
                ));

        return auditRepository
                .findAllByTransferIdOrderByCreatedAtAsc(transferId)
                .stream()
                .map(CustomerTimelineEventResponse::from)
                .toList();
    }

    private CurrentActor requireCustomer() {
        return currentActorService.requireRole(AppRoles.CUSTOMER);
    }

    private Beneficiary findActiveBeneficiary(
            UUID beneficiaryId,
            UUID customerId
    ) {
        return beneficiaryRepository
                .findByIdAndCustomerIdAndActiveTrue(
                        beneficiaryId,
                        customerId
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Active beneficiary not found."
                ));
    }

    private FxQuote findOwnedQuote(UUID quoteId, UUID customerId) {
        return fxQuoteRepository
                .findByIdAndCustomerId(quoteId, customerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "FX quote not found."
                ));
    }

    private void validateQuoteMatchesBeneficiary(
            FxQuote quote,
            Beneficiary beneficiary
    ) {
        if (!beneficiary.getDestinationCurrency()
                .equals(quote.getDestinationCurrency())) {
            throw new IllegalArgumentException(
                    "Beneficiary currency does not match FX quote."
            );
        }

        if (!isSupportedDestination(
                beneficiary.getDestinationCountry(),
                beneficiary.getDestinationCurrency()
        )) {
            throw new IllegalArgumentException(
                    "Unsupported destination country and currency combination."
            );
        }
    }

    private void validateQuoteMatchesSnapshot(
            FxQuote quote,
            Transfer transfer
    ) {
        if (!quote.getSourceCurrency().equals(transfer.getSourceCurrency())
                || !quote.getDestinationCurrency()
                .equals(transfer.getDestinationCurrency())
                || quote.getSourceAmount()
                .compareTo(transfer.getSourceAmount()) != 0
                || quote.getDestinationAmount()
                .compareTo(transfer.getDestinationAmount()) != 0
                || quote.getExchangeRate()
                .compareTo(transfer.getExchangeRate()) != 0) {
            throw new IllegalStateException(
                    "FX quote no longer matches the draft transfer."
            );
        }
    }

    private void validateAmount(String currency, java.math.BigDecimal amount) {
        FxProperties.AmountLimit limit = fxProperties.amountLimits()
                .get(currency);
        Integer scale = fxProperties.minorUnits().get(currency);

        if (limit == null || scale == null) {
            throw new IllegalStateException(
                    "Amount configuration is missing for " + currency + "."
            );
        }

        if (amount.compareTo(limit.minimum()) < 0
                || amount.compareTo(limit.maximum()) > 0) {
            throw new IllegalArgumentException(
                    "Source amount is outside the permitted range."
            );
        }

        if (amount.stripTrailingZeros().scale() > scale) {
            throw new IllegalArgumentException(
                    "Source amount exceeds the permitted currency precision."
            );
        }
    }

    private String validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key header is required."
            );
        }

        String trimmed = idempotencyKey.trim();

        if (trimmed.length() > 128) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must not exceed 128 characters."
            );
        }

        return trimmed;
    }

    private String validateMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "A response message is required."
            );
        }

        String trimmed = message.trim();

        if (trimmed.length() > 1000) {
            throw new IllegalArgumentException(
                    "Response must not exceed 1000 characters."
            );
        }

        return trimmed;
    }

    private String submissionFingerprint(Transfer transfer) {
        return RequestFingerprint.sha256(String.join("|",
                transfer.getId().toString(),
                transfer.getBeneficiaryId().toString(),
                transfer.getFxQuoteId().toString(),
                transfer.getSourceCurrency(),
                transfer.getSourceAmount().stripTrailingZeros()
                        .toPlainString(),
                transfer.getDestinationCurrency(),
                transfer.getDestinationAmount().stripTrailingZeros()
                        .toPlainString(),
                transfer.getExchangeRate().stripTrailingZeros()
                        .toPlainString()
        ));
    }

    private String quoteSource(FxQuote quote) {
        return "mock".equals(quote.getProvider()) ? "MOCK" : "EXTERNAL";
    }

    private boolean isSupportedDestination(String country, String currency) {
        return switch (country) {
            case "US" -> currency.equals("USD");
            case "DE", "FR", "IT", "ES", "NL" -> currency.equals("EUR");
            case "GB" -> currency.equals("GBP");
            case "AU" -> currency.equals("AUD");
            case "NZ" -> currency.equals("NZD");
            case "JP" -> currency.equals("JPY");
            case "IN" -> currency.equals("INR");
            default -> false;
        };
    }
}
