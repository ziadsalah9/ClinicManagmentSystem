package com.clinic.backend.scheduling.infrastructure.persistence;

import com.clinic.backend.scheduling.domain.model.ShiftBreak;
import com.clinic.backend.scheduling.domain.model.TimeRange;
import com.clinic.backend.scheduling.domain.model.WeeklyShiftRule;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "weekly_rule", schema = "scheduling",
        uniqueConstraints = @UniqueConstraint(name = "uk_scheduling_weekly_rule_day",
                columnNames = {"shift_id", "day_of_week"}))
public class WeeklyShiftRuleJpaEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private ScheduleShiftJpaEntity shift;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 12)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "follow_up_quota", nullable = false)
    private int followUpQuota;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<ShiftBreakJpaEntity> breaks = new ArrayList<>();

    protected WeeklyShiftRuleJpaEntity() { }

    WeeklyShiftRuleJpaEntity(ScheduleShiftJpaEntity shift, WeeklyShiftRule rule) {
        this.id = UUID.randomUUID();
        this.shift = shift;
        apply(rule);
    }

    void apply(WeeklyShiftRule rule) {
        this.dayOfWeek = rule.dayOfWeek();
        this.startTime = rule.workingHours().start();
        this.endTime = rule.workingHours().end();
        this.followUpQuota = rule.followUpQuota();
        this.breaks.clear();
        for (ShiftBreak shiftBreak : rule.breaks()) {
            this.breaks.add(new ShiftBreakJpaEntity(this, shiftBreak));
        }
    }

    WeeklyShiftRule toDomain() {
        List<ShiftBreak> mappedBreaks = breaks.stream()
                .sorted(Comparator.comparing(ShiftBreakJpaEntity::getStartTime))
                .map(ShiftBreakJpaEntity::toDomain)
                .toList();
        return new WeeklyShiftRule(dayOfWeek, new TimeRange(startTime, endTime),
                followUpQuota, mappedBreaks);
    }

    DayOfWeek getDayOfWeek() { return dayOfWeek; }
}
