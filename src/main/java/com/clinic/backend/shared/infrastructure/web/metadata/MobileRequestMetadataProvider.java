package com.clinic.backend.shared.infrastructure.web.metadata;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

/**
 * Exposes metadata for the current HTTP request without relying on a custom ThreadLocal.
 * Background jobs, scheduled tasks, and async executor threads deliberately return empty.
 */
@Component
public class MobileRequestMetadataProvider {
    static final String REQUEST_ATTRIBUTE =
            MobileRequestMetadataProvider.class.getName() + ".MOBILE_METADATA";

    public Optional<MobileRequestMetadata> current() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.empty();
        }
        Object value = attributes.getAttribute(REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        return value instanceof MobileRequestMetadata metadata ? Optional.of(metadata) : Optional.empty();
    }
}
