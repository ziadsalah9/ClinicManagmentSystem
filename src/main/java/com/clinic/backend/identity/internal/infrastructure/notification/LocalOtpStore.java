package com.clinic.backend.identity.internal.infrastructure.notification;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("local")
public class LocalOtpStore {
    private final ConcurrentHashMap<UUID, Entry> byChallengeId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Entry> latestByPhone = new ConcurrentHashMap<>();

    public void record(UUID challengeId, String phone, String code) {
        Entry entry = new Entry(challengeId, phone, code, Instant.now());
        byChallengeId.put(challengeId, entry);
        latestByPhone.put(phone, entry);
    }

    public Optional<Entry> findByChallengeId(UUID challengeId) {
        return Optional.ofNullable(byChallengeId.get(challengeId));
    }

    public Optional<Entry> findLatestByPhone(String phone) {
        return Optional.ofNullable(latestByPhone.get(phone));
    }

    public record Entry(UUID challengeId, String phone, String code, Instant capturedAt) {
    }
}
