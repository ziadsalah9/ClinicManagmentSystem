package com.clinic.backend.provisioning.internal.application;

import com.clinic.backend.clinic.ClinicFacade;
import com.clinic.backend.doctor.DoctorFacade;
import com.clinic.backend.doctor.DoctorInfo;
import com.clinic.backend.doctor.internal.domain.DoctorError;
import com.clinic.backend.doctor.internal.domain.DoctorException;
import com.clinic.backend.identity.IdentityFacade;
import com.clinic.backend.identity.IdentityMembership;
import com.clinic.backend.identity.IdentityUser;
import com.clinic.backend.identity.ProvisionedDoctorAccount;
import com.clinic.backend.provisioning.internal.domain.ProvisioningError;
import com.clinic.backend.provisioning.internal.domain.ProvisioningException;
import com.clinic.backend.provisioning.internal.presentation.ProvisioningApiModels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cross-module provisioning workflow. Validates target clinic and specialties via public
 * facades before any write, then creates identity account+membership and doctor profile in a
 * single transaction (REQUIRED propagation joins nested @Transactional module services).
 */
@Service
public class ProvisioningApplicationService {
    private static final Logger log = LoggerFactory.getLogger(ProvisioningApplicationService.class);

    private final IdentityFacade identity;
    private final DoctorFacade doctors;
    private final ClinicFacade clinics;
    private final PasswordEncoder passwordEncoder;

    public ProvisioningApplicationService(IdentityFacade identity, DoctorFacade doctors, ClinicFacade clinics, PasswordEncoder passwordEncoder) {
        this.identity = identity;
        this.doctors = doctors;
        this.clinics = clinics;
        this.passwordEncoder = passwordEncoder;
    }

    public record ProvisionedDoctorResult(IdentityUser user, IdentityMembership membership, DoctorInfo doctor) {
    }

    public record ClaimedDoctorResult(IdentityUser user, IdentityMembership membership, IdentityFacade.IssuedTokens tokens) {
    }

    @Transactional
    public ProvisionedDoctorResult provisionDoctor(UUID actorUserId, ProvisioningApiModels.ProvisionDoctorRequest request) {
        UUID clinicId = requireClinicId(request.clinicId());
        Set<UUID> specialtyIds = requireSpecialtyIds(request.specialtyIds());
        validateClinicAccess(actorUserId, clinicId);
        validateSpecialties(specialtyIds);

        ProvisionedDoctorAccount account = identity.provisionDoctorAccount(
                actorUserId, clinicId, request.phone(), request.fullName(), request.email(), request.password());
        DoctorInfo doctor = doctors.provisionDoctorProfile(account.user().id(), request.professionalTitle(), request.bio(),
                request.yearsExperience(), request.syndicateNumber(), request.gender(), specialtyIds);

        log.info("Provisioned doctor userId={} membershipId={} clinicId={} actorUserId={}",
                account.user().id(), account.membership().id(), clinicId, actorUserId);
        return new ProvisionedDoctorResult(account.user(), account.membership(), doctor);
    }

    @Transactional
    public ProvisionedDoctorResult provisionDoctorDirect(UUID actorUserId, ProvisioningApiModels.ProvisionDoctorRequest request) {
        UUID clinicId = requireClinicId(request.clinicId());
        Set<UUID> specialtyIds = requireSpecialtyIds(request.specialtyIds());
        validateClinicAccess(actorUserId, clinicId);
        validateSpecialties(specialtyIds);

        ProvisionedDoctorAccount account = identity.provisionDoctorAccountDirect(actorUserId, clinicId, request.phone(),
                request.fullName(), request.email(), passwordEncoder.encode(request.password()));
        DoctorInfo doctor = doctors.provisionDoctorProfile(account.user().id(), request.professionalTitle(), request.bio(), request.yearsExperience(), request.syndicateNumber(), request.gender(), specialtyIds);
        log.info("Direct-provisioned doctor userId={} membershipId={} clinicId={} actorUserId={} mustChangePassword=true", account.user().id(), account.membership().id(), clinicId, actorUserId);
        return new ProvisionedDoctorResult(account.user(), account.membership(), doctor);
    }

    @Transactional
    public IdentityFacade.DoctorInviteOutcome inviteDoctor(UUID actorUserId, ProvisioningApiModels.InviteDoctorRequest request) {
        UUID clinicId = requireClinicId(request.clinicId());
        validateClinicAccess(actorUserId, clinicId);
        String clinicName = clinics.findClinic(clinicId)
                .map(com.clinic.backend.clinic.ClinicReference::name)
                .orElseThrow(() -> new ProvisioningException(ProvisioningError.CLINIC_NOT_FOUND,
                        "Clinic was not found for clinicId=" + clinicId));
        var outcome = identity.inviteOrAddDoctor(actorUserId, clinicId, clinicName,
                request.phone(), request.fullName(), request.email());
        if (outcome.invited()) {
            log.info("Created lightweight doctor invite inviteId={} clinicId={} actorUserId={}", outcome.inviteId(), clinicId, actorUserId);
        } else {
            log.info("Linked existing doctor userId={} to clinicId={} actorUserId={} (no new account/invite token)", outcome.user().id(), clinicId, actorUserId);
        }
        return outcome;
    }

    @Transactional
    public ClaimedDoctorResult claimDoctor(ProvisioningApiModels.ClaimDoctorRequest request) {
        var claim = identity.claimDoctorOnboarding(request.token(), request.password(), request.locale());
        var tokens = identity.issueTokens(claim.user().id(), request.deviceId());
        log.info("Claimed doctor onboarding userId={} clinicId={} profileCompleted=false", claim.user().id(), claim.clinicId());
        return new ClaimedDoctorResult(claim.user(), claim.membership(), tokens);
    }

    private void validateClinicAccess(UUID actorUserId, UUID clinicId) {
        if (!clinics.existsById(clinicId)) {
            throw new ProvisioningException(ProvisioningError.CLINIC_NOT_FOUND,
                    "Clinic was not found for clinicId=" + clinicId);
        }
        if (actorUserId != null && !identity.isSystemAdmin(actorUserId) && identity.hasAnyActiveRole(actorUserId, "OWNER")) {
            if (!clinics.isOwnedBy(clinicId, actorUserId)) {
                throw new ProvisioningException(ProvisioningError.ACCESS_DENIED,
                        "Clinic owner can only provision doctors for clinics they own");
            }
        }
    }

    private void validateSpecialties(Set<UUID> specialtyIds) {
        try {
            doctors.assertActiveSpecialtiesExist(specialtyIds);
        } catch (DoctorException ex) {
            if (ex.error() == DoctorError.SPECIALTY_NOT_FOUND) {
                throw new ProvisioningException(ProvisioningError.SPECIALTY_NOT_FOUND, ex.getMessage());
            }
            if (ex.error() == DoctorError.INVALID_INPUT) {
                throw new ProvisioningException(ProvisioningError.INVALID_INPUT, ex.getMessage());
            }
            throw ex;
        }
    }

    private static UUID requireClinicId(UUID clinicId) {
        if (clinicId == null) {
            throw new ProvisioningException(ProvisioningError.INVALID_INPUT, "clinicId is required");
        }
        return clinicId;
    }

    private static Set<UUID> requireSpecialtyIds(Set<UUID> specialtyIds) {
        if (specialtyIds == null || specialtyIds.isEmpty() || specialtyIds.stream().anyMatch(Objects::isNull)) {
            throw new ProvisioningException(ProvisioningError.INVALID_INPUT, "Select between 1 and 8 valid specialties");
        }
        Set<UUID> ids = specialtyIds.stream().collect(Collectors.toUnmodifiableSet());
        if (ids.size() > 8) {
            throw new ProvisioningException(ProvisioningError.INVALID_INPUT, "Select between 1 and 8 valid specialties");
        }
        return ids;
    }

}
