package com.clinic.backend.shared.infrastructure.web.metadata;

import com.clinic.backend.config.SecurityConfiguration;
import com.clinic.backend.identity.IdentityFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MobileRequestMetadataSecurityTest.ProbeController.class)
@Import({
        SecurityConfiguration.class,
        MobileRequestMetadataFilter.class,
        MobileRequestMetadataProperties.class,
        MobileRequestMetadataProvider.class,
        MobileRequestMetadataSecurityTest.ProbeController.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = "app.mobile-metadata.required-path-patterns=/api/v1/**")
class MobileRequestMetadataSecurityTest {
    private static final String DEVICE_ID = "8f3a9c12-7d21-4e35-a821-123456789abc";
    private static final String SUBJECT = UUID.randomUUID().toString();

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationContext context;

    @MockitoBean
    IdentityFacade identity;

    @Test
    void mobileHeadersWithoutJwtDoNotAuthenticate() throws Exception {
        mvc.perform(withMobileHeaders(get("/api/v1/probe/secure")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validJwtWithMobileHeadersIsAuthenticated() throws Exception {
        mvc.perform(withMobileHeaders(get("/api/v1/probe/secure"))
                        .with(jwt().jwt(token -> token.subject(SUBJECT))
                                .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                .andExpect(status().isOk());
    }

    @Test
    void validJwtWithoutMobileHeadersIsRejectedByMetadataPolicy() throws Exception {
        mvc.perform(get("/api/v1/probe/secure")
                        .with(jwt().jwt(token -> token.subject(SUBJECT))
                                .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidMetadataCannotBypassMetadataValidation() throws Exception {
        mvc.perform(get("/api/v1/probe/secure")
                        .header(MobileRequestHeaders.SOURCE_MOBILE_APP, "android")
                        .header(MobileRequestHeaders.APP_VERSION, "1.4.0")
                        .header(MobileRequestHeaders.OS_VERSION, "Android 15")
                        .header(MobileRequestHeaders.DEVICE_ID, "not-a-uuid")
                        .with(jwt().jwt(token -> token.subject(SUBJECT))
                                .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validMetadataDoesNotBypassRoleAuthorization() throws Exception {
        mvc.perform(withMobileHeaders(get("/api/v1/probe/doctor"))
                        .with(jwt().jwt(token -> token.subject(SUBJECT))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void validMetadataWithRequiredRoleIsAuthorized() throws Exception {
        mvc.perform(withMobileHeaders(get("/api/v1/probe/doctor"))
                        .with(jwt().jwt(token -> token.subject(SUBJECT))
                                .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                .andExpect(status().isOk());
    }

    @Test
    void filterIsRegisteredAsASingleBean() {
        assertThat(context.getBeansOfType(MobileRequestMetadataFilter.class)).hasSize(1);
    }

    private static MockHttpServletRequestBuilder withMobileHeaders(MockHttpServletRequestBuilder request) {
        return request
                .header(MobileRequestHeaders.SOURCE_MOBILE_APP, "android")
                .header(MobileRequestHeaders.APP_VERSION, "1.4.0")
                .header(MobileRequestHeaders.OS_VERSION, "Android 15")
                .header(MobileRequestHeaders.DEVICE_ID, DEVICE_ID);
    }

    @RestController
    @RequestMapping("/api/v1/probe")
    public static class ProbeController {
        @GetMapping("/secure")
        String secure() {
            return "ok";
        }

        @GetMapping("/doctor")
        @PreAuthorize("hasRole('DOCTOR')")
        String doctorOnly() {
            return "ok";
        }
    }
}
