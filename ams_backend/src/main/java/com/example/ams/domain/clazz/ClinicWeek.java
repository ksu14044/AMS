package com.example.ams.domain.clazz;

import java.time.Instant;
import java.time.LocalDate;

public record ClinicWeek(long classId, LocalDate weekStartDate, ClinicWeekStatus status, Instant lockedAt) {
}
