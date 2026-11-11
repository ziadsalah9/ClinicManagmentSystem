package com.clinic.backend.clinic.internal.application;

import com.clinic.backend.clinic.internal.domain.Area;
import com.clinic.backend.clinic.internal.domain.Clinic;
import com.clinic.backend.clinic.internal.domain.ClinicVerificationEvent;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicRepository {
    Optional<Clinic> findById(UUID clinicId);

    boolean existsById(UUID clinicId);

    boolean existsOwnedBy(UUID clinicId, UUID ownerUserId);

    List<Clinic> findByIds(Collection<UUID> clinicIds);

    List<Clinic> findAll();

    Clinic save(Clinic clinic);

    ClinicVerificationEvent saveVerificationEvent(ClinicVerificationEvent event);

    List<ClinicVerificationEvent> findVerificationEvents(UUID clinicId);

    Optional<Area> findActiveAreaById(UUID areaId);

    List<Area> findActiveAreas(String city);

    List<Area> findAreasByIds(Collection<UUID> areaIds);

    record CreateIdempotencyClaim(boolean newClaim, UUID clinicId) {
    }

    CreateIdempotencyClaim claimCreateRequest(UUID actorUserId, String key, String requestHash);

    void completeCreateRequest(UUID actorUserId, String key, UUID clinicId);
}
