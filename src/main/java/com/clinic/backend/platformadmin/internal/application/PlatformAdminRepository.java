package com.clinic.backend.platformadmin.internal.application;

import com.clinic.backend.platformadmin.internal.domain.AdminAuditRecord;
import com.clinic.backend.platformadmin.internal.domain.PlatformAdminGrant;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PlatformAdminRepository {
    Optional<PlatformAdminGrant> findByUserId(UUID userId);
    Optional<PlatformAdminGrant> findByUserIdForUpdate(UUID userId);
    PlatformAdminGrant save(PlatformAdminGrant grant);
    void saveAudit(AdminAuditRecord audit);
    Set<String> findActivePermissionNames(UUID userId);
    boolean hasAnyActiveGrant();
}
