package com.clinic.backend.identity;

import java.util.UUID;

/** Public, persistence-agnostic identity projection. */
public record IdentityUser(UUID id, String phone, String fullName, String status) {
}
