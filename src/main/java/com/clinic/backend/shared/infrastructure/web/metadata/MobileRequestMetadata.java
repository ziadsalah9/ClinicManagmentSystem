package com.clinic.backend.shared.infrastructure.web.metadata;

import java.util.Objects;

/**
 * Immutable, untrusted client metadata extracted from a mobile request.
 * It must never be used as proof of identity or as an authorization input.
 */
public record MobileRequestMetadata(
        String sourceMobileApp,
        String appVersion,
        String osVersion,
        String deviceId
) {
    public MobileRequestMetadata {
        Objects.requireNonNull(sourceMobileApp, "sourceMobileApp");
        Objects.requireNonNull(appVersion, "appVersion");
        Objects.requireNonNull(osVersion, "osVersion");
        Objects.requireNonNull(deviceId, "deviceId");
    }
}
