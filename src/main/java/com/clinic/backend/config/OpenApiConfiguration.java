package com.clinic.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class OpenApiConfiguration {
    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI clinicOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Clinic Backend API")
                        .version("v1")
                        .description("Local API documentation. Use Authorize with a JWT access token (without the 'Bearer ' prefix)."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .name(BEARER)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
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
