package com.clinic.backend.clinic.internal.infrastructure.persistence;

import com.clinic.backend.clinic.internal.application.ClinicRepository;
import com.clinic.backend.clinic.internal.domain.Area;
import com.clinic.backend.clinic.internal.domain.Clinic;
import com.clinic.backend.clinic.internal.domain.ClinicError;
import com.clinic.backend.clinic.internal.domain.ClinicException;
import com.clinic.backend.clinic.internal.domain.ClinicVerificationEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaClinicRepository implements ClinicRepository {
    private final SpringDataClinicRepository clinics;
    private final SpringDataAreaRepository areas;
    private final SpringDataClinicVerificationEventRepository verificationEvents;
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;

    public JpaClinicRepository(SpringDataClinicRepository clinics,
                               SpringDataAreaRepository areas,
                               SpringDataClinicVerificationEventRepository verificationEvents,
                               JdbcTemplate jdbc,
                               EntityManager entityManager) {
        this.clinics = clinics;
        this.areas = areas;
        this.verificationEvents = verificationEvents;
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Clinic> findById(UUID clinicId) {
        return clinics.findWithPhonesById(clinicId).map(ClinicJpaEntity::toDomain);
    }

    @Override
    public boolean existsById(UUID clinicId) {
        return clinicId != null && clinics.existsById(clinicId);
    }

    @Override
    public boolean existsOwnedBy(UUID clinicId, UUID ownerUserId) {
        return clinicId != null && ownerUserId != null && clinics.existsByOwnerUserIdAndId(ownerUserId, clinicId);
    }

    @Override
    public List<Clinic> findByIds(Collection<UUID> clinicIds) {
        if (clinicIds == null || clinicIds.isEmpty()) return List.of();
        return clinics.findAllWithPhonesByIdIn(clinicIds).stream().map(ClinicJpaEntity::toDomain).toList();
    }

    @Override
    public List<Clinic> findAll() {
        return clinics.findAllWithPhones().stream().map(ClinicJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public Clinic save(Clinic clinic) {
        Optional<ClinicJpaEntity> existing = clinics.findWithPhonesById(clinic.id());
        ClinicJpaEntity entity = existing.orElseGet(() -> new ClinicJpaEntity(clinic));
        if (existing.isPresent()) {
            // ElementCollection-only changes must also advance the aggregate version.
            entityManager.lock(entity, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        }
        entity.apply(clinic);
        return clinics.saveAndFlush(entity).toDomain();
    }

    @Override
    @Transactional
    public ClinicVerificationEvent saveVerificationEvent(ClinicVerificationEvent event) {
        return verificationEvents.saveAndFlush(new ClinicVerificationEventJpaEntity(event)).toDomain();
    }

    @Override
    public List<ClinicVerificationEvent> findVerificationEvents(UUID clinicId) {
        return verificationEvents.findByClinicIdOrderByCreatedAtDesc(clinicId).stream()
                .map(ClinicVerificationEventJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<Area> findActiveAreaById(UUID areaId) {
        return areas.findByIdAndActiveTrue(areaId).map(AreaJpaEntity::toDomain);
    }

    @Override
    public List<Area> findActiveAreas(String city) {
        List<AreaJpaEntity> found = city == null || city.isBlank()
                ? areas.findAllByActiveTrueOrderByCityAscNameEnAsc()
                : areas.findAllByActiveTrueAndCityIgnoreCaseOrderByNameEnAsc(city.trim());
        return found.stream().map(AreaJpaEntity::toDomain).toList();
    }

    @Override
    public List<Area> findAreasByIds(Collection<UUID> areaIds) {
        if (areaIds == null || areaIds.isEmpty()) return List.of();
        return areas.findAllByIdIn(areaIds).stream().map(AreaJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public CreateIdempotencyClaim claimCreateRequest(UUID actorUserId, String key, String requestHash) {
        int inserted = jdbc.update("""
                insert into clinic.create_idempotency (actor_user_id, idempotency_key, request_hash)
                values (?, ?, ?)
                on conflict (actor_user_id, idempotency_key) do nothing
                """, actorUserId, key, requestHash);

        return jdbc.queryForObject("""
                select request_hash, clinic_id
                from clinic.create_idempotency
                where actor_user_id = ? and idempotency_key = ?
                for update
                """, (rs, rowNum) -> {
            String storedHash = rs.getString("request_hash");
            UUID resourceId = rs.getObject("clinic_id", UUID.class);
            if (!storedHash.equals(requestHash)) {
                throw new ClinicException(ClinicError.IDEMPOTENCY_KEY_REUSED,
                        "Idempotency-Key was already used with a different request body");
            }
            if (inserted == 1) return new CreateIdempotencyClaim(true, null);
            if (resourceId == null) throw new ClinicException(ClinicError.IDEMPOTENCY_IN_PROGRESS,
                    "The original request has not completed");
            return new CreateIdempotencyClaim(false, resourceId);
        }, actorUserId, key);
    }

    @Override
    @Transactional
    public void completeCreateRequest(UUID actorUserId, String key, UUID clinicId) {
        int changed = jdbc.update("""
                update clinic.create_idempotency set clinic_id = ?
                where actor_user_id = ? and idempotency_key = ? and clinic_id is null
                """, clinicId, actorUserId, key);
        if (changed != 1) throw new ClinicException(ClinicError.IDEMPOTENCY_IN_PROGRESS,
                "Could not complete the clinic creation idempotency record");
    }
}
