package com.example.ams.api.dto;

import java.time.Instant;

import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.Homework;
import com.example.ams.service.AssignmentTargetService.TargetView;

public record HomeworkResponse(
		long homeworkId,
		long classId,
		String title,
		Integer questionCount,
		AssignmentStatus status,
		Instant createdAt,
		AssignmentTargetResponse targets) {

	public static HomeworkResponse from(Homework homework, TargetView targets) {
		return new HomeworkResponse(
				homework.homeworkId(),
				homework.classId(),
				homework.title(),
				homework.questionCount(),
				homework.status(),
				homework.createdAt(),
				AssignmentTargetResponse.from(targets));
	}
}
