package com.clinic.backend.shared.infrastructure.web.metadata;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class MobileRequestMetadataProviderTest {
    private final MobileRequestMetadataProvider provider = new MobileRequestMetadataProvider();

    @AfterEach
    void cleanUp() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void returnsEmptyOutsideAnHttpRequest() {
        assertThat(provider.current()).isEmpty();
    }

    @Test
    void returnsMetadataAttachedToTheCurrentRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MobileRequestMetadata metadata = new MobileRequestMetadata(
                "ios", "2.0.1", "iOS 18.1", "8f3a9c12-7d21-4e35-a821-123456789abc");
        request.setAttribute(MobileRequestMetadataProvider.REQUEST_ATTRIBUTE, metadata);

        assertThat(provider.current()).contains(metadata);
    }

    @Test
    void ignoresUnexpectedAttributeValues() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        request.setAttribute(MobileRequestMetadataProvider.REQUEST_ATTRIBUTE, "wrong-type");

        assertThat(provider.current()).isEmpty();
    }
}
