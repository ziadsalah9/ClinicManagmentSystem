package com.clinic.backend.shared.infrastructure.web.metadata;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "app.mobile-metadata")
public class MobileRequestMetadataProperties {
    /**
     * Paths that always require mobile metadata even when the client sends no mobile header at all.
     * Header-bearing requests are classified as mobile regardless of this list.
     */
    private List<String> requiredPathPatterns = List.of();

    /**
     * Technical endpoints that are never treated as mobile API requests.
     */
    private List<String> excludedPathPatterns = List.of(
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/**",
            "/error"
    );

    /**
     * Optional contract allowlist. An empty set accepts any non-blank source value.
     * Configure this only when the mobile team confirms the exact allowed values.
     */
    private Set<String> allowedSourceApps = Set.of();

    /**
     * Device identifier format. The default matches the UUID example in the mobile contract.
     */
    private String deviceIdPattern =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    public List<String> getRequiredPathPatterns() {
        return requiredPathPatterns;
    }

    public void setRequiredPathPatterns(List<String> requiredPathPatterns) {
        this.requiredPathPatterns = requiredPathPatterns == null ? List.of() : List.copyOf(requiredPathPatterns);
    }

    public List<String> getExcludedPathPatterns() {
        return excludedPathPatterns;
    }

    public void setExcludedPathPatterns(List<String> excludedPathPatterns) {
        this.excludedPathPatterns = excludedPathPatterns == null ? List.of() : List.copyOf(excludedPathPatterns);
    }

    public Set<String> getAllowedSourceApps() {
        return allowedSourceApps;
    }

    public void setAllowedSourceApps(Set<String> allowedSourceApps) {
        this.allowedSourceApps = allowedSourceApps == null ? Set.of() : Set.copyOf(allowedSourceApps);
    }

    public String getDeviceIdPattern() {
        return deviceIdPattern;
    }

    public void setDeviceIdPattern(String deviceIdPattern) {
        this.deviceIdPattern = deviceIdPattern;
    }
}
