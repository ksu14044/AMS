package com.example.ams.event;

public record HomeworkResultUpdatedEvent(long classId, long homeworkId, long studentId, String title) {
}
