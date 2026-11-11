package com.clinic.backend.shared.infrastructure.web.metadata;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MobileRequestMetadataFilterTest {
    private static final JsonMapper JSON = new JsonMapper();
    private static final String DEVICE_ID = UUID.randomUUID().toString();

    private final MobileRequestMetadataProvider provider = new MobileRequestMetadataProvider();

    @AfterEach
    void cleanUp() {
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @Test
    void readsAndTrimsAllFourHeaders() throws Exception {
        MobileRequestMetadataFilter filter = filter(new MobileRequestMetadataProperties());
        MockHttpServletRequest request = request("/api/v1/somewhere");
        addHeaders(request);
        request.removeHeader(MobileRequestHeaders.SOURCE_MOBILE_APP);
        request.addHeader(MobileRequestHeaders.SOURCE_MOBILE_APP, "  android  ");
        request.removeHeader(MobileRequestHeaders.APP_VERSION);
        request.addHeader(MobileRequestHeaders.APP_VERSION, " 1.4.0 ");
        request.removeHeader(MobileRequestHeaders.OS_VERSION);
        request.addHeader(MobileRequestHeaders.OS_VERSION, " Android 15 ");
        request.removeHeader(MobileRequestHeaders.DEVICE_ID);
        request.addHeader(MobileRequestHeaders.DEVICE_ID, " " + DEVICE_ID + " ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<MobileRequestMetadata> captured = new AtomicReference<>();
        filter.doFilter(request, response, (req, res) -> captured.set(provider.current().orElse(null)));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(captured.get()).isEqualTo(
                new MobileRequestMetadata("android", "1.4.0", "Android 15", DEVICE_ID));
    }

    @Test
    void missingHeadersReturnProblemDetailWithMissingNames() throws Exception {
        MobileRequestMetadataFilter filter = filter(new MobileRequestMetadataProperties());
        MockHttpServletRequest request = request("/api/v1/somewhere");
        request.addHeader(MobileRequestHeaders.SOURCE_MOBILE_APP, "android");
        request.addHeader(MobileRequestHeaders.OS_VERSION, "Android 15");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Boolean> downstream = new AtomicReference<>(false);
        filter.doFilter(request, response, (req, res) -> downstream.set(true));

        assertThat(downstream).hasValue(false);
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        Map<String, Object> body = body(response);
        assertThat(body)
                .containsEntry("title", "Invalid Request Metadata")
                .containsEntry("status", 400)
                .containsEntry("code", "MOBILE_METADATA_REQUIRED")
                .containsEntry("detail", "Required mobile request headers are missing.");
        assertThat(body.get("missingHeaders"))
                .isEqualTo(List.of(MobileRequestHeaders.APP_VERSION, MobileRequestHeaders.DEVICE_ID));
    }

    @Test
    void blankHeadersAreRejected() throws Exception {
        MobileRequestMetadataFilter filter = filter(new MobileRequestMetadataProperties());
        MockHttpServletRequest request = request("/api/v1/somewhere");
        addHeaders(request);
        request.removeHeader(MobileRequestHeaders.APP_VERSION);
        request.addHeader(MobileRequestHeaders.APP_VERSION, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(body(response).get("missingHeaders"))
                .isEqualTo(List.of(MobileRequestHeaders.APP_VERSION));
    }

    @Test
    void oversizedAndControlCharacterValuesAreRejected() throws Exception {
        MobileRequestMetadataFilter filter = filter(new MobileRequestMetadataProperties());
        MockHttpServletRequest request = request("/api/v1/somewhere");
        addHeaders(request);
        request.removeHeader(MobileRequestHeaders.OS_VERSION);
        request.addHeader(MobileRequestHeaders.OS_VERSION, "a".repeat(65));
        request.removeHeader(MobileRequestHeaders.SOURCE_MOBILE_APP);
        request.addHeader(MobileRequestHeaders.SOURCE_MOBILE_APP, "and\nroid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getStatus()).isEqualTo(400);
        Map<String, Object> body = body(response);
        assertThat(body).containsEntry("code", "MOBILE_METADATA_INVALID");
        assertThat(body.get("invalidHeaders"))
                .isEqualTo(List.of(MobileRequestHeaders.SOURCE_MOBILE_APP, MobileRequestHeaders.OS_VERSION));
    }

    @Test
    void malformedDeviceIdIsRejected() throws Exception {
        MobileRequestMetadataFilter filter = filter(new MobileRequestMetadataProperties());
        MockHttpServletRequest request = request("/api/v1/somewhere");
        addHeaders(request);
        request.removeHeader(MobileRequestHeaders.DEVICE_ID);
        request.addHeader(MobileRequestHeaders.DEVICE_ID, "not-a-uuid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(body(response).get("invalidHeaders"))
                .isEqualTo(List.of(MobileRequestHeaders.DEVICE_ID));
    }

    @Test
    void configuredSourceAllowlistIsEnforcedCaseInsensitively() throws Exception {
        MobileRequestMetadataProperties properties = new MobileRequestMetadataProperties();
        properties.setAllowedSourceApps(Set.of("android", "ios"));
        MobileRequestMetadataFilter filter = filter(properties);

        MockHttpServletRequest accepted = request("/api/v1/somewhere");
        addHeaders(accepted);
        accepted.removeHeader(MobileRequestHeaders.SOURCE_MOBILE_APP);
        accepted.addHeader(MobileRequestHeaders.SOURCE_MOBILE_APP, "ANDROID");
        MockHttpServletResponse acceptedResponse = new MockHttpServletResponse();
        filter.doFilter(accepted, acceptedResponse, (req, res) -> { });
        assertThat(acceptedResponse.getStatus()).isEqualTo(200);

        MockHttpServletRequest rejected = request("/api/v1/somewhere");
        addHeaders(rejected);
        rejected.removeHeader(MobileRequestHeaders.SOURCE_MOBILE_APP);
        rejected.addHeader(MobileRequestHeaders.SOURCE_MOBILE_APP, "windows");
        MockHttpServletResponse rejectedResponse = new MockHttpServletResponse();
        filter.doFilter(rejected, rejectedResponse, (req, res) -> { });
        assertThat(rejectedResponse.getStatus()).isEqualTo(400);
        assertThat(body(rejectedResponse).get("invalidHeaders"))
                .isEqualTo(List.of(MobileRequestHeaders.SOURCE_MOBILE_APP));
    }

    @Test
    void configuredRequiredPathRejectsRequestsWithNoMobileHeaders() throws Exception {
        MobileRequestMetadataProperties properties = new MobileRequestMetadataProperties();
        properties.setRequiredPathPatterns(List.of("/api/v1/mobile/**"));
        MobileRequestMetadataFilter filter = filter(properties);
        MockHttpServletRequest request = request("/api/v1/mobile/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(body(response).get("missingHeaders")).isEqualTo(List.of(
                MobileRequestHeaders.SOURCE_MOBILE_APP,
                MobileRequestHeaders.APP_VERSION,
                MobileRequestHeaders.OS_VERSION,
                MobileRequestHeaders.DEVICE_ID));
    }

    @Test
    void nonMobileRequestsWithoutAnyMobileHeaderContinue() throws Exception {
        MobileRequestMetadataProperties properties = new MobileRequestMetadataProperties();
        properties.setRequiredPathPatterns(List.of("/api/v1/mobile/**"));
        MobileRequestMetadataFilter filter = filter(properties);
        MockHttpServletRequest request = request("/api/v1/clinics");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Boolean> downstream = new AtomicReference<>(false);
        AtomicReference<MobileRequestMetadata> captured = new AtomicReference<>();
        filter.doFilter(request, response, (req, res) -> {
            downstream.set(true);
            captured.set(provider.current().orElse(null));
        });

        assertThat(downstream).hasValue(true);
        assertThat(captured).hasValue(null);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void excludedTechnicalPathsBypassValidationEvenWhenPathPolicyIsBroad() throws Exception {
        MobileRequestMetadataProperties properties = new MobileRequestMetadataProperties();
        properties.setRequiredPathPatterns(List.of("/**"));
        MobileRequestMetadataFilter filter = filter(properties);

        for (String path : List.of("/actuator/health", "/swagger-ui.html", "/v3/api-docs",
                "/swagger-ui/index.html", "/error")) {
            MockHttpServletRequest request = request(path);
            MockHttpServletResponse response = new MockHttpServletResponse();
            AtomicReference<Boolean> downstream = new AtomicReference<>(false);

            filter.doFilter(request, response, (req, res) -> downstream.set(true));

            assertThat(downstream).as(path).hasValue(true);
            assertThat(response.getStatus()).as(path).isEqualTo(200);
        }
    }

    @Test
    void mdcIsAvailableDownstreamAndClearedAfterNormalRequest() throws Exception {
        MobileRequestMetadataFilter filter = filter(new MobileRequestMetadataProperties());
        MockHttpServletRequest request = request("/api/v1/somewhere");
        addHeaders(request);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Map<String, String>> mdcDuringRequest = new AtomicReference<>();
        filter.doFilter(request, response, (req, res) ->
                mdcDuringRequest.set(MDC.getCopyOfContextMap()));

        assertThat(mdcDuringRequest.get()).containsEntry("mobile.source-app", "android")
                .containsEntry("mobile.app-version", "1.4.0")
                .containsEntry("mobile.os-version", "Android 15")
                .containsEntry("mobile.device-id", DEVICE_ID);
        assertThat(MDC.get("mobile.source-app")).isNull();
        assertThat(MDC.get("mobile.app-version")).isNull();
        assertThat(MDC.get("mobile.os-version")).isNull();
        assertThat(MDC.get("mobile.device-id")).isNull();
    }

    @Test
    void mdcIsClearedAfterDownstreamException() {
        MobileRequestMetadataFilter filter = filter(new MobileRequestMetadataProperties());
        MockHttpServletRequest request = request("/api/v1/somewhere");
        addHeaders(request);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain failingChain = (req, res) -> {
            throw new ServletException("boom");
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, failingChain))
                .isInstanceOf(ServletException.class);

        assertThat(MDC.get("mobile.source-app")).isNull();
        assertThat(MDC.get("mobile.app-version")).isNull();
        assertThat(MDC.get("mobile.os-version")).isNull();
        assertThat(MDC.get("mobile.device-id")).isNull();
    }

    @Test
    void metadataDoesNotLeakBetweenRequestsOnTheSameThread() throws Exception {
        MobileRequestMetadataFilter filter = filter(new MobileRequestMetadataProperties());

        MockHttpServletRequest first = request("/api/v1/somewhere");
        addHeaders(first);
        filter.doFilter(first, new MockHttpServletResponse(), (req, res) -> { });

        MockHttpServletRequest second = request("/api/v1/clinics");
        AtomicReference<MobileRequestMetadata> captured = new AtomicReference<>();
        filter.doFilter(second, new MockHttpServletResponse(), (req, res) ->
                captured.set(provider.current().orElse(null)));

        assertThat(captured).hasValue(null);
        assertThat(MDC.get("mobile.source-app")).isNull();
    }

    private MobileRequestMetadataFilter filter(MobileRequestMetadataProperties properties) {
        return new MobileRequestMetadataFilter(JSON, properties);
    }

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }

    private static void addHeaders(MockHttpServletRequest request) {
        request.addHeader(MobileRequestHeaders.SOURCE_MOBILE_APP, "android");
        request.addHeader(MobileRequestHeaders.APP_VERSION, "1.4.0");
        request.addHeader(MobileRequestHeaders.OS_VERSION, "Android 15");
        request.addHeader(MobileRequestHeaders.DEVICE_ID, DEVICE_ID);
    }

    private static Map<String, Object> body(MockHttpServletResponse response) throws Exception {
        return JSON.readValue(response.getContentAsByteArray(), new TypeReference<>() { });
    }
}
