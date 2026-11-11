package com.clinic.backend.clinic.internal.domain;

import java.util.Set;

/** Explicit verification operations. Arbitrary status assignment is not supported. */
public enum ClinicVerificationAction {
    APPROVE(ClinicStatus.ACTIVE, Set.of(ClinicStatus.PENDING_VERIFICATION), true, false),
    REJECT(ClinicStatus.REJECTED, Set.of(ClinicStatus.PENDING_VERIFICATION), true, true),
    REQUEST_INFORMATION(null, Set.of(ClinicStatus.PENDING_VERIFICATION), false, true),
    SUSPEND(ClinicStatus.SUSPENDED, Set.of(ClinicStatus.ACTIVE), true, true),
    REACTIVATE(ClinicStatus.ACTIVE, Set.of(ClinicStatus.SUSPENDED), true, false);

    private final ClinicStatus targetStatus;
    private final Set<ClinicStatus> allowedFrom;
    private final boolean changesStatus;
    private final boolean requiresReason;

    ClinicVerificationAction(ClinicStatus targetStatus, Set<ClinicStatus> allowedFrom,
                             boolean changesStatus, boolean requiresReason) {
        this.targetStatus = targetStatus;
        this.allowedFrom = allowedFrom;
        this.changesStatus = changesStatus;
        this.requiresReason = requiresReason;
    }

    public ClinicStatus targetStatus() {
        return targetStatus;
    }

    public Set<ClinicStatus> allowedFrom() {
        return allowedFrom;
    }

    public boolean changesStatus() {
        return changesStatus;
    }

    public boolean requiresReason() {
        return requiresReason;
    }
}
