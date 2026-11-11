package com.clinic.backend.scheduling.infrastructure.persistence;

import com.clinic.backend.scheduling.domain.model.CapacityMode;
import com.clinic.backend.scheduling.domain.model.ScheduleShift;
import com.clinic.backend.scheduling.domain.model.ShiftBreak;
import com.clinic.backend.scheduling.domain.model.TimeRange;
import com.clinic.backend.scheduling.domain.model.WeeklyShiftRule;
import com.clinic.backend.shared.AuditedJpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shift", schema = "scheduling")
public class ScheduleShiftJpaEntity extends AuditedJpaEntity {
    @Id
    private UUID id;

    @Column(name = "doctor_clinic_id", nullable = false, updatable = false)
    private UUID doctorClinicId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "capacity_mode", nullable = false, length = 20)
    private CapacityMode capacityMode;

    @Column(name = "average_visit_minutes")
    private Integer averageVisitMinutes;

    @Column(name = "fixed_capacity")
    private Integer fixedCapacity;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "active", nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<WeeklyShiftRuleJpaEntity> weeklyRules = new ArrayList<>();

    protected ScheduleShiftJpaEntity() { }

    public ScheduleShiftJpaEntity(ScheduleShift shift) {
        this.id = shift.id();
        this.doctorClinicId = shift.doctorClinicId();
        apply(shift);
    }

    public void apply(ScheduleShift shift) {
        this.name = shift.name();
        this.capacityMode = shift.capacityMode();
        this.averageVisitMinutes = shift.averageVisitMinutes();
        this.fixedCapacity = shift.fixedCapacity();
        this.effectiveFrom = shift.effectiveFrom();
        this.active = shift.active();
        this.weeklyRules.clear();
        for (WeeklyShiftRule rule : shift.weeklyRules()) {
            this.weeklyRules.add(new WeeklyShiftRuleJpaEntity(this, rule));
        }
    }

    public ScheduleShift toDomain() {
        List<WeeklyShiftRule> rules = weeklyRules.stream()
                .sorted(Comparator.comparing(WeeklyShiftRuleJpaEntity::getDayOfWeek))
                .map(WeeklyShiftRuleJpaEntity::toDomain)
                .toList();
        return ScheduleShift.restore(id, doctorClinicId, name, capacityMode, averageVisitMinutes,
                fixedCapacity, effectiveFrom, rules, active);
    }

    public UUID getId() { return id; }
    public UUID getDoctorClinicId() { return doctorClinicId; }
    public boolean isActive() { return active; }
}
