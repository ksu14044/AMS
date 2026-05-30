package com.example.ams.api.dto;

import java.time.Instant;

import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.Homework;

public record HomeworkResponse(
		long homeworkId,
		long classId,
		String title,
		Integer questionCount,
		AssignmentStatus status,
		Instant createdAt) {

	public static HomeworkResponse from(Homework homework) {
		return new HomeworkResponse(
				homework.homeworkId(),
				homework.classId(),
				homework.title(),
				homework.questionCount(),
				homework.status(),
				homework.createdAt());
	}
}
