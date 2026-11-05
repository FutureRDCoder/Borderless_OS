package com.borderlessos.compliance.controller;

import com.borderlessos.compliance.dto.request.ReasonRequest;
import com.borderlessos.compliance.dto.response.ComplianceDecisionResponse;
import com.borderlessos.compliance.dto.response.ComplianceHoldResponse;
import com.borderlessos.compliance.service.ComplianceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/compliance")
@PreAuthorize("hasRole('COMPLIANCE')")
public class ComplianceController {

    private final ComplianceService complianceService;

    public ComplianceController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @GetMapping("/queue")
    public ResponseEntity<List<ComplianceHoldResponse>> getQueue() {
        return ResponseEntity.ok(complianceService.getActiveHolds());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Long>> getDashboard() {
        return ResponseEntity.ok(Map.of(
                "activeHolds",
                complianceService.countActiveHolds()
        ));
    }

    @GetMapping("/transfers/{id}")
    public ResponseEntity<ComplianceHoldResponse> getHold(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(complianceService.getHold(id));
    }

    @GetMapping("/transfers/{id}/decisions")
    public ResponseEntity<List<ComplianceDecisionResponse>> getDecisions(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(complianceService.getDecisions(id));
    }

    @GetMapping("/audit-events")
    public ResponseEntity<List<ComplianceDecisionResponse>> getAuditEvents(
            @RequestParam(required = false) UUID transferId
    ) {
        if (transferId != null) {
            return ResponseEntity.ok(
                    complianceService.getDecisions(transferId)
            );
        }

        return ResponseEntity.ok(complianceService.getRecentDecisions());
    }

    @PostMapping("/transfers/{id}/place-hold")
    public ResponseEntity<ComplianceHoldResponse> placeHold(
            @PathVariable UUID id,
            @Valid @RequestBody ReasonRequest request
    ) {
        return ResponseEntity.ok(
                complianceService.placeHold(id, request.reason())
        );
    }

    @PostMapping("/transfers/{id}/maintain-hold")
    public ResponseEntity<ComplianceHoldResponse> maintainHold(
            @PathVariable UUID id,
            @Valid @RequestBody ReasonRequest request
    ) {
        return ResponseEntity.ok(
                complianceService.maintainHold(id, request.reason())
        );
    }

    @PostMapping("/transfers/{id}/clear-hold")
    public ResponseEntity<ComplianceHoldResponse> clearHold(
            @PathVariable UUID id,
            @Valid @RequestBody ReasonRequest request
    ) {
        return ResponseEntity.ok(
                complianceService.clearHold(id, request.reason())
        );
    }
}
