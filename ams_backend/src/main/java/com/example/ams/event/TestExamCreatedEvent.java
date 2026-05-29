package com.example.ams.event;

public record TestExamCreatedEvent(long classId, long testId, String title) {
}
