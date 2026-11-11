package com.clinic.backend.shared.infrastructure.web.metadata;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Central mobile metadata extraction/validation for servlet requests.
 *
 * <p>Security runs before this filter: these headers never participate in authentication or
 * authorization. The filter only validates untrusted client metadata when a request is
 * classified as mobile-originated, and exposes safe values to downstream code.</p>
 */
@Component
@Order(MobileRequestMetadataFilter.FILTER_ORDER)
public class MobileRequestMetadataFilter extends OncePerRequestFilter {
    /**
     * Spring Security's filter chain runs at {@code -100}; a higher value keeps metadata
     * validation behind authentication and method security, without registering a duplicate
     * filter inside the security chain.
     */
    public static final int FILTER_ORDER = Ordered.LOWEST_PRECEDENCE - 1000;

    static final String MDC_SOURCE_MOBILE_APP = "mobile.source-app";
    static final String MDC_APP_VERSION = "mobile.app-version";
    static final String MDC_OS_VERSION = "mobile.os-version";
    static final String MDC_DEVICE_ID = "mobile.device-id";

    private static final int MAX_SOURCE_MOBILE_APP_LENGTH = 32;
    private static final int MAX_APP_VERSION_LENGTH = 32;
    private static final int MAX_OS_VERSION_LENGTH = 64;
    private static final int MAX_DEVICE_ID_LENGTH = 128;

    private final ObjectMapper problemObjectMapper;
    private final MobileRequestMetadataProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Pattern deviceIdPattern;

    public MobileRequestMetadataFilter(ObjectMapper objectMapper, MobileRequestMetadataProperties properties) {
        this.problemObjectMapper = objectMapper.rebuild()
                .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
                .build();
        this.properties = properties;
        String configuredPattern = properties.getDeviceIdPattern();
        if (configuredPattern == null || configuredPattern.isBlank()) {
            throw new IllegalStateException("app.mobile-metadata.device-id-pattern must not be blank");
        }
        try {
            this.deviceIdPattern = Pattern.compile(configuredPattern);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("app.mobile-metadata.device-id-pattern is invalid", exception);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return matchesAny(path(request), properties.getExcludedPathPatterns());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        clearMdc();
        try {
            String path = path(request);
            boolean requiredByPath = matchesAny(path, properties.getRequiredPathPatterns());
            boolean hasMobileHeader = hasAnyMobileHeader(request);
            if (!requiredByPath && !hasMobileHeader) {
                filterChain.doFilter(request, response);
                return;
            }

            List<String> missing = new ArrayList<>();
            List<String> invalid = new ArrayList<>();
            String sourceMobileApp = readHeader(request, MobileRequestHeaders.SOURCE_MOBILE_APP,
                    MAX_SOURCE_MOBILE_APP_LENGTH, missing, invalid);
            String appVersion = readHeader(request, MobileRequestHeaders.APP_VERSION,
                    MAX_APP_VERSION_LENGTH, missing, invalid);
            String osVersion = readHeader(request, MobileRequestHeaders.OS_VERSION,
                    MAX_OS_VERSION_LENGTH, missing, invalid);
            String deviceId = readHeader(request, MobileRequestHeaders.DEVICE_ID,
                    MAX_DEVICE_ID_LENGTH, missing, invalid);

            validateSourceMobileApp(sourceMobileApp, invalid);
            validateDeviceId(deviceId, invalid);

            if (!missing.isEmpty() || !invalid.isEmpty()) {
                writeProblem(request, response, missing, invalid);
                return;
            }

            MobileRequestMetadata metadata = new MobileRequestMetadata(
                    sourceMobileApp, appVersion, osVersion, deviceId);
            request.setAttribute(MobileRequestMetadataProvider.REQUEST_ATTRIBUTE, metadata);
            putMdc(metadata);
            filterChain.doFilter(request, response);
        } finally {
            clearMdc();
        }
    }

    private String readHeader(HttpServletRequest request, String headerName, int maxLength,
                              List<String> missing, List<String> invalid) {
        String raw = request.getHeader(headerName);
        if (raw == null) {
            missing.add(headerName);
            return null;
        }
        String value = raw.strip();
        if (value.isEmpty()) {
            missing.add(headerName);
            return null;
        }
        if (value.length() > maxLength || containsIsoControl(value)) {
            invalid.add(headerName);
            return null;
        }
        return value;
    }

    private void validateSourceMobileApp(String sourceMobileApp, List<String> invalid) {
        if (sourceMobileApp == null) {
            return;
        }
        Set<String> allowed = properties.getAllowedSourceApps();
        if (!allowed.isEmpty()
                && allowed.stream().noneMatch(value -> value.equalsIgnoreCase(sourceMobileApp))) {
            invalid.add(MobileRequestHeaders.SOURCE_MOBILE_APP);
        }
    }

    private void validateDeviceId(String deviceId, List<String> invalid) {
        if (deviceId != null && !deviceIdPattern.matcher(deviceId).matches()) {
            invalid.add(MobileRequestHeaders.DEVICE_ID);
        }
    }

    private boolean hasAnyMobileHeader(HttpServletRequest request) {
        return request.getHeader(MobileRequestHeaders.SOURCE_MOBILE_APP) != null
                || request.getHeader(MobileRequestHeaders.APP_VERSION) != null
                || request.getHeader(MobileRequestHeaders.OS_VERSION) != null
                || request.getHeader(MobileRequestHeaders.DEVICE_ID) != null;
    }

    private boolean matchesAny(String path, List<String> patterns) {
        for (String pattern : patterns) {
            if (pattern != null && pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private static String path(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri.isEmpty() ? "/" : uri;
    }

    private static boolean containsIsoControl(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private void writeProblem(HttpServletRequest request, HttpServletResponse response,
                              List<String> missing, List<String> invalid) throws IOException {
        boolean hasMissing = !missing.isEmpty();
        String code = hasMissing ? "MOBILE_METADATA_REQUIRED" : "MOBILE_METADATA_INVALID";
        String detail = hasMissing
                ? "Required mobile request headers are missing."
                : "Mobile request headers are invalid.";

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Invalid Request Metadata");
        problem.setType(URI.create("https://api.clinic.local/problems/common/"
                + code.toLowerCase().replace('_', '-')));
        problem.setProperty("code", code);
        if (hasMissing) {
            problem.setProperty("missingHeaders", List.copyOf(missing));
        }
        if (!invalid.isEmpty()) {
            problem.setProperty("invalidHeaders", List.copyOf(invalid));
        }

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        problemObjectMapper.writeValue(response.getOutputStream(), problem);
    }

    private static void putMdc(MobileRequestMetadata metadata) {
        MDC.put(MDC_SOURCE_MOBILE_APP, metadata.sourceMobileApp());
        MDC.put(MDC_APP_VERSION, metadata.appVersion());
        MDC.put(MDC_OS_VERSION, metadata.osVersion());
        MDC.put(MDC_DEVICE_ID, metadata.deviceId());
    }

    private static void clearMdc() {
        MDC.remove(MDC_SOURCE_MOBILE_APP);
        MDC.remove(MDC_APP_VERSION);
        MDC.remove(MDC_OS_VERSION);
        MDC.remove(MDC_DEVICE_ID);
    }
}
