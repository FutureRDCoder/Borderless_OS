package com.borderlessos.admin.controller;

import com.borderlessos.admin.dto.response.AdminUserResponse;
import com.borderlessos.admin.service.AdminService;
import com.borderlessos.operations.dto.response.AuditEventResponse;
import com.borderlessos.operations.dto.response.OperationsTransferResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole(@appRoles.admin())")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Long>> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getUsers() {
        return ResponseEntity.ok(adminService.getUsers());
    }

    @GetMapping("/transfers")
    public ResponseEntity<List<OperationsTransferResponse>> getTransfers() {
        return ResponseEntity.ok(adminService.getTransfers());
    }

    @GetMapping("/audit-events")
    public ResponseEntity<List<AuditEventResponse>> getAuditEvents(
            @RequestParam(required = false) UUID transferId
    ) {
        return ResponseEntity.ok(adminService.getAuditEvents(transferId));
    }
}
