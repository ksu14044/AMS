package com.example.ams.event;

public record VideoLessonCreatedEvent(long classId, long videoId, String title) {
}
