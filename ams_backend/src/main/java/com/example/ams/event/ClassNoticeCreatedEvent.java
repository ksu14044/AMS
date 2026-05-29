package com.example.ams.event;

public record ClassNoticeCreatedEvent(long classId, long noticeId, String noticeTitle) {
}
