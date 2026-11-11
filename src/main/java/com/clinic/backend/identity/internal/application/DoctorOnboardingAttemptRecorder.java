package com.clinic.backend.identity.internal.application;

import com.clinic.backend.identity.internal.application.port.IdentityStore;
import com.clinic.backend.identity.internal.domain.DoctorOnboardingInvite;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records failed onboarding attempts in an isolated transaction so lockout persists on validation errors. */
@Component
public class DoctorOnboardingAttemptRecorder {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(DoctorOnboardingInvite invite, IdentityStore store) {
        store.saveDoctorOnboardingInvite(invite);
    }
}