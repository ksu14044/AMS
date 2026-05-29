package com.example.ams.event;

public record ClinicResultUpdatedEvent(long classId, long reservationId, long studentId, String slotLabel) {
}
