package com.example.ams.event;

public record HomeworkCreatedEvent(long classId, long homeworkId, String title) {
}
