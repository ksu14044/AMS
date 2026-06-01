package com.example.ams.event;

public record VideoCertificationSubmittedEvent(long classId, long videoId, long studentId, String title) {
}
