package com.clinic.backend.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class IdentityPersistenceIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.15")
            .withDatabaseName("clinic_test")
            .withUsername("clinic")
            .withPassword("clinic");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesIdentitySchemaAndPhoneUniquenessIsEnforcedByPostgres() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        String phone = "+201011223344";

        jdbcTemplate.update("""
                insert into identity.app_user
                (id, phone, full_name, status, locale, created_at, updated_at, version)
                values (?, ?, ?, 'ACTIVE', 'ar', now(), now(), 0)
                """, firstId, phone, "First Owner");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into identity.app_user
                (id, phone, full_name, status, locale, created_at, updated_at, version)
                values (?, ?, ?, 'ACTIVE', 'ar', now(), now(), 0)
                """, secondId, phone, "Second Owner"))
                .isInstanceOf(RuntimeException.class);
    }
}
