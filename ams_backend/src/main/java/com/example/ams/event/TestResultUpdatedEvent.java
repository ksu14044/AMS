package com.example.ams.event;

public record TestResultUpdatedEvent(long classId, long testId, long studentId, String title) {
}
