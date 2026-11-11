package com.clinic.backend.config;

import com.clinic.backend.shared.infrastructure.web.metadata.MobileRequestHeaders;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class OpenApiConfiguration {
    private static final String BEARER = "bearerAuth";
    private static final String SOURCE_MOBILE_APP_PARAMETER = "MobileSourceMobileApp";
    private static final String APP_VERSION_PARAMETER = "MobileAppVersion";
    private static final String OS_VERSION_PARAMETER = "MobileOsVersion";
    private static final String DEVICE_ID_PARAMETER = "MobileDeviceId";

    @Bean
    OpenAPI clinicOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Clinic Backend API")
                        .version("v1")
                        .description("""
                                Local API documentation. Use Authorize with a JWT access token (without the 'Bearer ' prefix).
                                All mobile-originated /api/v1 requests must include source-mobile-app, app-version,
                                os-version, and X-DEVICE-ID. These headers are client metadata, not authentication.
                                Non-mobile and system clients may omit them unless a path is configured as mobile-only.
                                Invalid or missing mobile metadata returns 400 with code MOBILE_METADATA_REQUIRED
                                or MOBILE_METADATA_INVALID, plus missingHeaders/invalidHeaders when applicable.
                                """))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .name(BEARER)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    GlobalOpenApiCustomizer mobileRequestMetadataOpenApiCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }
            components.addParameters(SOURCE_MOBILE_APP_PARAMETER, header(
                    MobileRequestHeaders.SOURCE_MOBILE_APP,
                    "Required for mobile requests. Allowed values follow the mobile team contract. "
                            + "Examples: Android 'android', iOS 'ios'.",
                    32));
            components.addParameters(APP_VERSION_PARAMETER, header(
                    MobileRequestHeaders.APP_VERSION,
                    "Required for mobile requests. Client build version; not parsed as strict SemVer. "
                            + "Android example: 1.4.0. iOS example: 1.4.0.",
                    32));
            components.addParameters(OS_VERSION_PARAMETER, header(
                    MobileRequestHeaders.OS_VERSION,
                    "Required for mobile requests. Android example: Android 15. iOS example: iOS 18.1.",
                    64));
            components.addParameters(DEVICE_ID_PARAMETER, header(
                    MobileRequestHeaders.DEVICE_ID,
                    "Required for mobile requests. Stable app installation UUID; never use as an identity or "
                            + "authorization credential. Example: 8f3a9c12-7d21-4e35-a821-123456789abc.",
                    128));

            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) -> {
                if (!path.startsWith("/api/v1/")) {
                    return;
                }
                pathItem.readOperations().forEach(operation -> operation
                        .addParametersItem(parameterRef(SOURCE_MOBILE_APP_PARAMETER))
                        .addParametersItem(parameterRef(APP_VERSION_PARAMETER))
                        .addParametersItem(parameterRef(OS_VERSION_PARAMETER))
                        .addParametersItem(parameterRef(DEVICE_ID_PARAMETER)));
            });
        };
    }

    private static Parameter header(String name, String description, int maxLength) {
        return new Parameter()
                .name(name)
                .in("header")
                .required(true)
                .description(description)
                .schema(new StringSchema().maxLength(maxLength));
    }

    private static Parameter parameterRef(String name) {
        return new Parameter().$ref("#/components/parameters/" + name);
    }

    @Bean
    GroupedOpenApi platformAdminApi() {
        return GroupedOpenApi.builder().group("platform-admin").pathsToMatch("/api/platform-admin/**").build();
    }

    @Bean
    GroupedOpenApi appApi() {
        return GroupedOpenApi.builder().group("app").pathsToMatch("/api/v1/**", "/actuator/health/**").build();
    }
}
