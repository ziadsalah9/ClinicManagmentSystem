package com.clinic.backend.identity;

public record ProvisionedDoctorAccount(IdentityUser user, IdentityMembership membership) {
}
