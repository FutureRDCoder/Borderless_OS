package com.borderlessos.transfer.service;

import com.borderlessos.transfer.enums.TransferStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.borderlessos.transfer.enums.TransferStatus.*;

public final class TransferStateMachine {

    private static final Map<TransferStatus, Set<TransferStatus>> ALLOWED =
            new EnumMap<>(TransferStatus.class);

    static {
        ALLOWED.put(DRAFT, EnumSet.of(SUBMITTED));
        ALLOWED.put(SUBMITTED, EnumSet.of(UNDER_REVIEW));
        ALLOWED.put(UNDER_REVIEW, EnumSet.of(
                NEEDS_INFO,
                COMPLIANCE_HOLD,
                READY_FOR_PROVIDER,
                REJECTED
        ));
        ALLOWED.put(NEEDS_INFO, EnumSet.of(SUBMITTED));
        ALLOWED.put(COMPLIANCE_HOLD, EnumSet.of(UNDER_REVIEW));
        ALLOWED.put(READY_FOR_PROVIDER, EnumSet.of(PROCESSING));
        ALLOWED.put(PROCESSING, EnumSet.of(UNKNOWN, SUCCEEDED, FAILED));
        ALLOWED.put(UNKNOWN, EnumSet.of(SUCCEEDED, FAILED));
        ALLOWED.put(SUCCEEDED, EnumSet.noneOf(TransferStatus.class));
        ALLOWED.put(FAILED, EnumSet.noneOf(TransferStatus.class));
        ALLOWED.put(REJECTED, EnumSet.noneOf(TransferStatus.class));
    }

    private TransferStateMachine() {
    }

    public static boolean canTransition(
            TransferStatus current,
            TransferStatus next
    ) {
        return ALLOWED.getOrDefault(current, Set.of()).contains(next);
    }

    public static void requireTransition(
            TransferStatus current,
            TransferStatus next
    ) {
        if (!canTransition(current, next)) {
            throw new IllegalStateException(
                    "Invalid transfer transition from " + current
                            + " to " + next + "."
            );
        }
    }

    public static boolean isTerminal(TransferStatus status) {
        return status == SUCCEEDED
                || status == FAILED
                || status == REJECTED;
    }
}
