package com.borderlessos.transfer.service;

import com.borderlessos.common.security.CurrentActor;
import com.borderlessos.transfer.entity.Transfer;
import com.borderlessos.transfer.entity.TransferAuditEvent;
import com.borderlessos.transfer.enums.TransferEventType;
import com.borderlessos.transfer.enums.TransferStatus;
import com.borderlessos.transfer.repository.TransferAuditEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TransferWorkflowService {

    private final TransferAuditEventRepository auditRepository;

    public TransferWorkflowService(
            TransferAuditEventRepository auditRepository
    ) {
        this.auditRepository = auditRepository;
    }

    public void transition(
            Transfer transfer,
            TransferStatus nextStatus,
            TransferEventType eventType,
            CurrentActor actor,
            String reason,
            String safeSummary,
            Instant now
    ) {
        TransferStatus previousStatus = transfer.getStatus();

        TransferStateMachine.requireTransition(previousStatus, nextStatus);

        transfer.transitionTo(nextStatus, now);

        auditRepository.save(TransferAuditEvent.create(
                transfer.getId(),
                actor.id(),
                actor.email(),
                actor.role(),
                eventType,
                previousStatus,
                nextStatus,
                reason,
                safeSummary,
                now
        ));
    }

    public void record(
            Transfer transfer,
            TransferEventType eventType,
            CurrentActor actor,
            String reason,
            String safeSummary,
            Instant now
    ) {
        auditRepository.save(TransferAuditEvent.create(
                transfer.getId(),
                actor.id(),
                actor.email(),
                actor.role(),
                eventType,
                transfer.getStatus(),
                transfer.getStatus(),
                reason,
                safeSummary,
                now
        ));
    }
}
