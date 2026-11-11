package com.clinic.backend.clinic.internal.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClinicVerificationDomainTest {
    private Clinic pending() {
        return Clinic.create(UUID.randomUUID(), "Clinic", "Address", UUID.randomUUID(),
                "Africa/Cairo", List.of("+201000000001"));
    }

    @Test
    void approveMovesPendingClinicToActive() {
        Clinic approved = pending().applyVerificationAction(ClinicVerificationAction.APPROVE, null);

        assertThat(approved.status()).isEqualTo(ClinicStatus.ACTIVE);
    }

    @Test
    void rejectRequiresReasonAndIsTerminal() {
        Clinic rejected = pending().applyVerificationAction(
                ClinicVerificationAction.REJECT, "Registration details are invalid");

        assertThat(rejected.status()).isEqualTo(ClinicStatus.REJECTED);
        assertThatThrownBy(() -> rejected.applyVerificationAction(
                ClinicVerificationAction.REACTIVATE, null))
                .isInstanceOf(ClinicException.class)
                .extracting(exception -> ((ClinicException) exception).error())
                .isEqualTo(ClinicError.INVALID_STATUS_TRANSITION);
    }

    @Test
    void informationRequestKeepsPendingStatus() {
        Clinic pending = pending().applyVerificationAction(
                ClinicVerificationAction.REQUEST_INFORMATION, "Upload the clinic license");

        assertThat(pending.status()).isEqualTo(ClinicStatus.PENDING_VERIFICATION);
    }

    @Test
    void suspendAndReactivateFollowActiveLifecycle() {
        Clinic active = pending().applyVerificationAction(ClinicVerificationAction.APPROVE, null);
        Clinic suspended = active.applyVerificationAction(
                ClinicVerificationAction.SUSPEND, "Temporary compliance hold");
        Clinic reactivated = suspended.applyVerificationAction(
                ClinicVerificationAction.REACTIVATE, null);

        assertThat(suspended.status()).isEqualTo(ClinicStatus.SUSPENDED);
        assertThat(reactivated.status()).isEqualTo(ClinicStatus.ACTIVE);
    }

    @Test
    void reasonIsRequiredForInformationRequestAndSuspension() {
        assertThatThrownBy(() -> pending().applyVerificationAction(
                ClinicVerificationAction.REQUEST_INFORMATION, " "))
                .isInstanceOf(ClinicException.class)
                .extracting(exception -> ((ClinicException) exception).error())
                .isEqualTo(ClinicError.VERIFICATION_REASON_REQUIRED);

        Clinic active = pending().applyVerificationAction(ClinicVerificationAction.APPROVE, null);
        assertThatThrownBy(() -> active.applyVerificationAction(ClinicVerificationAction.SUSPEND, null))
                .isInstanceOf(ClinicException.class)
                .extracting(exception -> ((ClinicException) exception).error())
                .isEqualTo(ClinicError.VERIFICATION_REASON_REQUIRED);
    }
}
