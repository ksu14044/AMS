package com.example.ams.event;

public record DiligenceReportCreatedEvent(long classId, long studentId, long reportId, String testTitle) {
}
