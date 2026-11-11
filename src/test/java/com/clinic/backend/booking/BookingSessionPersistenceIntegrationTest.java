package com.clinic.backend.booking;

import com.clinic.backend.booking.internal.application.BookingSessionService;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class BookingSessionPersistenceIntegrationTest {
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
    BookingSessionService sessions;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void synchronizationIsIdempotentAndPreservesBookedCapacity() {
        UUID doctorClinicId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 11, 9);
        List<BookingSessionSpecification> first = List.of(
                new BookingSessionSpecification(date, LocalTime.of(9, 0), LocalTime.of(12, 0), 12, 3),
                new BookingSessionSpecification(date.plusDays(1), LocalTime.of(9, 0), LocalTime.of(12, 0), 12, 3));

        BookingSessionSyncResult initial = sessions.synchronize(
                doctorClinicId, shiftId, first, date, date.plusDays(59));
        BookingSessionSyncResult repeated = sessions.synchronize(
                doctorClinicId, shiftId, first, date, date.plusDays(59));

        assertThat(initial.sessions()).hasSize(2);
        assertThat(repeated.sessions()).hasSize(2);
        assertThat(repeated.sessions()).extracting(BookingSessionInfo::id)
                .containsExactlyInAnyOrderElementsOf(initial.sessions().stream().map(BookingSessionInfo::id).toList());

        UUID bookedSessionId = initial.sessions().getFirst().id();
        jdbcTemplate.update("update booking.session set booked_count = 5 where id = ?", bookedSessionId);

        BookingSessionSyncResult reduced = sessions.synchronize(doctorClinicId, shiftId,
                List.of(new BookingSessionSpecification(date, LocalTime.of(9, 0), LocalTime.of(12, 0), 3, 1)),
                date, date.plusDays(59));

        assertThat(reduced.conflictingSessionIds()).contains(bookedSessionId);
        assertThat(reduced.sessions()).hasSize(1);
        assertThat(reduced.sessions().getFirst().capacity()).isEqualTo(12);
        assertThat(reduced.sessions().getFirst().bookedCount()).isEqualTo(5);
    }
}
