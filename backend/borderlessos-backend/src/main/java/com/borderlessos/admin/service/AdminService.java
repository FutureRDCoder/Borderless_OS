package com.borderlessos.admin.service;

import com.borderlessos.admin.dto.response.AdminUserResponse;
import com.borderlessos.identity.repository.UserRepository;
import com.borderlessos.operations.dto.response.AuditEventResponse;
import com.borderlessos.operations.dto.response.OperationsTransferResponse;
import com.borderlessos.transfer.enums.TransferStatus;
import com.borderlessos.transfer.repository.TransferAuditEventRepository;
import com.borderlessos.transfer.repository.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final TransferRepository transferRepository;
    private final TransferAuditEventRepository auditRepository;

    public AdminService(
            UserRepository userRepository,
            TransferRepository transferRepository,
            TransferAuditEventRepository auditRepository
    ) {
        this.userRepository = userRepository;
        this.transferRepository = transferRepository;
        this.auditRepository = auditRepository;
    }

    public List<AdminUserResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    public Map<String, Long> getDashboard() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("users", userRepository.count());
        counts.put("transfers", transferRepository.count());
        counts.put("drafts", transferRepository.countByStatus(
                TransferStatus.DRAFT
        ));
        counts.put("underReview", transferRepository.countByStatus(
                TransferStatus.UNDER_REVIEW
        ));
        counts.put("complianceHold", transferRepository.countByStatus(
                TransferStatus.COMPLIANCE_HOLD
        ));
        counts.put("processing", transferRepository.countByStatus(
                TransferStatus.PROCESSING
        ));
        counts.put("unknown", transferRepository.countByStatus(
                TransferStatus.UNKNOWN
        ));
        counts.put("succeeded", transferRepository.countByStatus(
                TransferStatus.SUCCEEDED
        ));
        counts.put("failed", transferRepository.countByStatus(
                TransferStatus.FAILED
        ));
        counts.put("rejected", transferRepository.countByStatus(
                TransferStatus.REJECTED
        ));
        return counts;
    }

    public List<OperationsTransferResponse> getTransfers() {
        return transferRepository
                .findTop200ByOrderByCreatedAtDesc()
                .stream()
                .map(OperationsTransferResponse::from)
                .toList();
    }

    public List<AuditEventResponse> getAuditEvents(UUID transferId) {
        if (transferId != null) {
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
}
