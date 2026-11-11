package com.clinic.backend.clinic;

import com.clinic.backend.clinic.internal.application.ClinicApplicationService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The only supported entry point for other modules that need clinic data. */
@Component
public class ClinicFacade {
    private final ClinicApplicationService service;

    public ClinicFacade(ClinicApplicationService service) {
        this.service = service;
    }

    public Optional<ClinicReference> findClinic(UUID clinicId) {
        return service.findClinicReference(clinicId);
    }

    public List<ClinicReference> findClinics(Collection<UUID> clinicIds) {
        return service.findClinicReferences(clinicIds);
    }

    public boolean existsById(UUID clinicId) {
        return service.existsClinic(clinicId);
    }

    public boolean isOwnedBy(UUID clinicId, UUID ownerUserId) {
        return service.isClinicOwnedBy(clinicId, ownerUserId);
    }

    public List<ClinicInfo> findAllClinicsForAdmin() {
        return service.findAllClinicsForAdmin();
    }

    public List<ClinicInfo> findAllClinicsForAdmin(String status, UUID areaId, UUID ownerUserId,
                                                   Instant createdFrom, Instant createdTo) {
        return service.findAllClinicsForAdmin(status, areaId, ownerUserId, createdFrom, createdTo);
    }

    public ClinicInfo getClinicForAdmin(UUID clinicId) {
        return service.getClinicForAdmin(clinicId);
    }

    public ClinicInfo transitionClinicVerification(UUID adminUserId, UUID clinicId, String action,
                                                   long expectedVersion, String reason) {
        return service.transitionClinicVerification(adminUserId, clinicId, action, expectedVersion, reason);
    }

    public List<ClinicVerificationEventInfo> findClinicVerificationHistory(UUID adminUserId, UUID clinicId) {
        return service.findVerificationHistory(adminUserId, clinicId);
    }

    public ClinicInfo adminUpdateClinic(UUID adminUserId, UUID clinicId, long expectedVersion, String name, String address,
                                        UUID areaId, String timeZone, List<String> phones) {
        return service.adminUpdateClinic(new com.clinic.backend.clinic.internal.application.ClinicCommands.UpdateClinic(
                adminUserId, clinicId, expectedVersion, name, address, areaId, timeZone, phones));
    }
}
