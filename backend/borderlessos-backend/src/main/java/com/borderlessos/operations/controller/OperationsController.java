package com.borderlessos.operations.controller;

import com.borderlessos.operations.dto.request.RejectTransferRequest;
import com.borderlessos.operations.dto.request.RequestInformationRequest;
import com.borderlessos.operations.dto.response.AuditEventResponse;
import com.borderlessos.operations.dto.response.OperationsTransferResponse;
import com.borderlessos.operations.service.OperationsService;
import com.borderlessos.provider.dto.response.ProviderAttemptResponse;
import com.borderlessos.transfer.dto.response.InformationRequestResponse;
import com.borderlessos.transfer.enums.TransferStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/operations")
@PreAuthorize("hasRole(@appRoles.operations())")
public class OperationsController {

    private final OperationsService operationsService;

    public OperationsController(OperationsService operationsService) {
        this.operationsService = operationsService;
    }

    @GetMapping("/queue")
    public ResponseEntity<List<OperationsTransferResponse>> getReviewQueue(
            @RequestParam(required = false) List<TransferStatus> status
    ) {
        return ResponseEntity.ok(operationsService.getQueue(status));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Long>> getDashboard() {
        return ResponseEntity.ok(operationsService.getDashboard());
    }

    @GetMapping("/transfers/{id}")
    public ResponseEntity<OperationsTransferResponse> getTransferDetails(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                operationsService.getTransferDetails(id)
        );
    }

    @PostMapping("/transfers/{id}/request-info")
    public ResponseEntity<OperationsTransferResponse> requestInformation(
            @PathVariable UUID id,
            @Valid @RequestBody RequestInformationRequest request
    ) {
        return ResponseEntity.ok(
                operationsService.requestInformation(id, request.message())
        );
    }

    @PostMapping("/transfers/{id}/reject")
    public ResponseEntity<OperationsTransferResponse> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectTransferRequest request
    ) {
        return ResponseEntity.ok(
                operationsService.reject(id, request.reason())
        );
    }

    @PostMapping("/transfers/{id}/mark-ready")
    public ResponseEntity<OperationsTransferResponse> markReady(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(operationsService.markReady(id));
    }

    @PostMapping("/transfers/{id}/dispatch")
    public ResponseEntity<ProviderAttemptResponse> dispatch(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(operationsService.dispatch(id));
    }

    @PostMapping("/transfers/{id}/provider-status-inquiry")
    public ResponseEntity<ProviderAttemptResponse> inquireProvider(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(operationsService.inquireProvider(id));
    }

    @GetMapping("/transfers/{id}/provider-attempt")
    public ResponseEntity<ProviderAttemptResponse> getProviderAttempt(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                operationsService.getProviderAttempt(id)
        );
    }

    @GetMapping("/transfers/{id}/information-requests")
    public ResponseEntity<List<InformationRequestResponse>>
    getInformationRequests(@PathVariable UUID id) {
        return ResponseEntity.ok(
                operationsService.getInformationRequests(id)
        );
    }

    @GetMapping("/audit-events")
    public ResponseEntity<List<AuditEventResponse>> getAuditEvents(
            @RequestParam(required = false) UUID transferId
    ) {
        return ResponseEntity.ok(
                operationsService.getAuditEvents(transferId)
        );
    }
}
