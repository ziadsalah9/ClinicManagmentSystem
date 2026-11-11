package com.clinic.backend.scheduling.infrastructure.persistence;

import com.clinic.backend.scheduling.domain.model.ShiftBreak;
import com.clinic.backend.scheduling.domain.model.TimeRange;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "shift_break", schema = "scheduling")
public class ShiftBreakJpaEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private WeeklyShiftRuleJpaEntity rule;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    protected ShiftBreakJpaEntity() { }

    ShiftBreakJpaEntity(WeeklyShiftRuleJpaEntity rule, ShiftBreak shiftBreak) {
        this.id = UUID.randomUUID();
        this.rule = rule;
        this.startTime = shiftBreak.timeRange().start();
        this.endTime = shiftBreak.timeRange().end();
    }

    ShiftBreak toDomain() {
        return new ShiftBreak(new TimeRange(startTime, endTime));
    }

    LocalTime getStartTime() { return startTime; }
}
