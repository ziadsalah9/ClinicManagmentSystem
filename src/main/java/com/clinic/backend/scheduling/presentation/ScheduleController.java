package com.clinic.backend.scheduling.presentation;

import com.clinic.backend.scheduling.application.ScheduleApplicationService;
import com.clinic.backend.scheduling.application.ScheduleViews;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/doctors/{doctorClinicId}/schedule")
class ScheduleController {
    private final ScheduleApplicationService schedules;

    ScheduleController(ScheduleApplicationService schedules) {
        this.schedules = schedules;
    }

    @GetMapping("/shifts")
    List<ScheduleViews.ShiftView> listShifts(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID clinicId,
                                             @PathVariable UUID doctorClinicId) {
        return schedules.listShifts(actor(jwt), clinicId, doctorClinicId);
    }

    @PostMapping("/shifts")
    @ResponseStatus(HttpStatus.CREATED)
    ScheduleViews.ShiftView createShift(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable UUID clinicId,
                                        @PathVariable UUID doctorClinicId,
                                        @Valid @RequestBody ScheduleApiModels.ShiftRequest request) {
        return schedules.createShift(actor(jwt), clinicId, doctorClinicId, request.toInput());
    }

    @PutMapping("/shifts/{shiftId}")
    ScheduleViews.ShiftView updateShift(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable UUID clinicId,
                                        @PathVariable UUID doctorClinicId,
                                        @PathVariable UUID shiftId,
                                        @Valid @RequestBody ScheduleApiModels.ShiftRequest request) {
        return schedules.updateShift(actor(jwt), clinicId, doctorClinicId, shiftId,
                request.toInput(), request.expectedVersion());
    }

    @PatchMapping("/shifts/{shiftId}/activation")
    ScheduleViews.ShiftView setActivation(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable UUID clinicId,
                                          @PathVariable UUID doctorClinicId,
                                          @PathVariable UUID shiftId,
                                          @Valid @RequestBody ScheduleApiModels.ActivationRequest request) {
        return schedules.setShiftActivation(actor(jwt), clinicId, doctorClinicId, shiftId,
                request.toInput());
    }

    @PostMapping("/previews/capacity")
    ScheduleViews.CapacityPreviewView previewCapacity(@AuthenticationPrincipal Jwt jwt,
                                                      @PathVariable UUID clinicId,
                                                      @PathVariable UUID doctorClinicId,
                                                      @Valid @RequestBody ScheduleApiModels.ShiftRequest request) {
        return schedules.previewCapacity(actor(jwt), clinicId, doctorClinicId, request.toInput());
    }

    @PostMapping("/shifts/{shiftId}/previews/change")
    ScheduleViews.ChangePreviewView previewChange(@AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable UUID clinicId,
                                                  @PathVariable UUID doctorClinicId,
                                                  @PathVariable UUID shiftId,
                                                  @Valid @RequestBody ScheduleApiModels.ShiftRequest request) {
        return schedules.previewChange(actor(jwt), clinicId, doctorClinicId, shiftId, request.toInput());
    }

    @GetMapping("/exceptions")
    List<ScheduleViews.ExceptionView> listExceptions(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable UUID clinicId,
                                                     @PathVariable UUID doctorClinicId) {
        return schedules.listExceptions(actor(jwt), clinicId, doctorClinicId);
    }

    @PostMapping("/exceptions")
    @ResponseStatus(HttpStatus.CREATED)
    ScheduleViews.ExceptionView createException(@AuthenticationPrincipal Jwt jwt,
                                                @PathVariable UUID clinicId,
                                                @PathVariable UUID doctorClinicId,
                                                @Valid @RequestBody ScheduleApiModels.ExceptionRequest request) {
        return schedules.createException(actor(jwt), clinicId, doctorClinicId, request.toInput());
    }

    @DeleteMapping("/exceptions/{exceptionId}")
    ScheduleViews.ExceptionView cancelException(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID clinicId,
            @PathVariable UUID doctorClinicId,
            @PathVariable UUID exceptionId,
            @RequestBody(required = false) @Valid ScheduleApiModels.CancelExceptionRequest request
    ) {
        return schedules.cancelException(actor(jwt), clinicId, doctorClinicId, exceptionId,
                request == null ? null : request.toInput());
    }

    @PostMapping("/sessions/generate")
    ScheduleViews.SessionGenerationView generateSessions(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable UUID clinicId,
                                                          @PathVariable UUID doctorClinicId) {
        return schedules.generateSessions(actor(jwt), clinicId, doctorClinicId);
    }

    @GetMapping("/sessions")
    List<ScheduleViews.SessionView> findSessions(@AuthenticationPrincipal Jwt jwt,
                                                 @PathVariable UUID clinicId,
                                                 @PathVariable UUID doctorClinicId,
                                                 @RequestParam LocalDate from,
                                                 @RequestParam LocalDate to) {
        return schedules.findSessions(actor(jwt), clinicId, doctorClinicId, from, to);
    }

    private static UUID actor(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
