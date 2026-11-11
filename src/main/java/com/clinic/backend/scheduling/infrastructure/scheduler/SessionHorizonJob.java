package com.clinic.backend.scheduling.infrastructure.scheduler;

import com.clinic.backend.doctorclinic.DoctorClinicFacade;
import com.clinic.backend.doctorclinic.DoctorClinicScheduleInfo;
import com.clinic.backend.scheduling.application.ScheduleHorizonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

@Component
public class SessionHorizonJob {
    private static final Logger log = LoggerFactory.getLogger(SessionHorizonJob.class);
    private static final String LOCK_KEY = "scheduling:session-horizon-job";

    private final DataSource dataSource;
    private final DoctorClinicFacade doctorClinics;
    private final ScheduleHorizonService horizon;

    public SessionHorizonJob(DataSource dataSource,
                             DoctorClinicFacade doctorClinics,
                             ScheduleHorizonService horizon) {
        this.dataSource = dataSource;
        this.doctorClinics = doctorClinics;
        this.horizon = horizon;
    }

    @Scheduled(cron = "${scheduling.horizon.cron:0 5 * * * *}")
    public void generateHorizon() {
        try (Connection lockConnection = dataSource.getConnection()) {
            if (!tryLock(lockConnection)) {
                log.debug("Skipping session horizon job because another instance owns the lock");
                return;
            }
            try {
                List<DoctorClinicScheduleInfo> associations = doctorClinics.findAllActiveForScheduling();
                for (DoctorClinicScheduleInfo association : associations) {
                    try {
                        horizon.generate(association.id());
                    } catch (RuntimeException exception) {
                        log.error("Session horizon generation failed for doctor-clinic {}",
                                association.id(), exception);
                    }
                }
            } finally {
                unlock(lockConnection);
            }
        } catch (Exception exception) {
            log.error("Session horizon job could not acquire or release its database lock", exception);
        }
    }

    private static boolean tryLock(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "select pg_try_advisory_lock(hashtextextended(?, 0))")) {
            statement.setString(1, LOCK_KEY);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }

    private static void unlock(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(
                "select pg_advisory_unlock(hashtextextended(?, 0))")) {
            statement.setString(1, LOCK_KEY);
            statement.executeQuery().close();
        } catch (Exception exception) {
            log.warn("Could not explicitly release session horizon job lock; the connection will release it", exception);
        }
    }
}
