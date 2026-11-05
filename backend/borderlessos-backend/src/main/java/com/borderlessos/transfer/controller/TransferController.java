package com.borderlessos.transfer.controller;

import com.borderlessos.transfer.dto.request.CreateTransferRequest;
import com.borderlessos.transfer.dto.request.RespondToInformationRequest;
import com.borderlessos.transfer.dto.response.CustomerTimelineEventResponse;
import com.borderlessos.transfer.dto.response.TransferResponse;
import com.borderlessos.transfer.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@PreAuthorize("hasRole('CUSTOMER')")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> createDraft(
            @Valid @RequestBody CreateTransferRequest request
    ) {
        TransferResponse response = transferService.createDraft(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TransferResponse> updateDraft(
            @PathVariable UUID id,
            @Valid @RequestBody CreateTransferRequest request
    ) {
        return ResponseEntity.ok(
                transferService.updateDraft(id, request)
        );
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<TransferResponse> submit(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return ResponseEntity.ok(
                transferService.submit(id, idempotencyKey)
        );
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<TransferResponse> respond(
            @PathVariable UUID id,
            @Valid @RequestBody RespondToInformationRequest request
    ) {
        return ResponseEntity.ok(
                transferService.respondToInformationRequest(
                        id,
                        request.message()
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<TransferResponse>> getMyTransfers() {
        return ResponseEntity.ok(transferService.getMyTransfers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getMyTransfer(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(transferService.getMyTransfer(id));
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<CustomerTimelineEventResponse>> getTimeline(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                transferService.getMyTransferTimeline(id)
        );
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<TransferResponse> getStatus(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(transferService.getMyTransfer(id));
    }
}
