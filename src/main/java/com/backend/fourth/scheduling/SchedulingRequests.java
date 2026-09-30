package com.backend.fourth.scheduling;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;

public final class SchedulingRequests {
    private SchedulingRequests() {}
    public record DailySlot(@NotNull LocalTime startTime, @NotNull LocalTime endTime) {}
    public record Period(@NotBlank @Size(max=120) String name,
                         @NotBlank @Pattern(regexp="\\d{4}/\\d{4}") String academicYear,
                         @NotNull @Min(1) @Max(2) Integer semester,
                         @NotBlank @Pattern(regexp="FINAL|SUPPLEMENTARY|SPECIAL") String examType,
                         @NotNull LocalDate startDate, @NotNull LocalDate endDate,
                         @NotEmpty @Size(max=7) List<@NotNull @Min(1) @Max(7) Integer> daysOfWeek,
                         @NotEmpty @Size(max=12) List<@NotNull @Valid DailySlot> slots) {}
    public record Course(@NotBlank @Size(max=15) String courseCode, @Min(1) @Max(1440) int durationMinutes) {}
    public record Selection(@Min(0) long revision, @NotEmpty @Size(max=100) List<@NotNull @Valid Course> courses) {}
    public record Generate(@Min(0) long revision, @Min(1) @Max(1000000) int searchLimit) {}
    public record Revision(@Min(0) long revision) {}
    public record Edit(@Min(0) long revision, @NotNull LocalDate examDate, @NotNull LocalTime startTime,
                       @NotEmpty @Size(max=100) List<@NotNull @Positive Integer> venueIds) {}
    public record Capacity(@Min(1) int examinationCapacity) {}
    public record Unavailability(@NotNull LocalDateTime startsAt, @NotNull LocalDateTime endsAt,
                                 @NotBlank @Size(max=500) String reason) {}
    public record Assignment(@Positive int examSessionId, @Positive int venueId, @Positive int staffId) {}
}
