package com.example.ams.api.dto;

import com.example.ams.domain.clazz.Homework;

public record HomeworkAnswerKeyResponse(
		int questionCount,
		boolean hasAnswerKeyFile) {

	public static HomeworkAnswerKeyResponse from(Homework homework) {
		int count = homework.questionCount() != null ? homework.questionCount() : 0;
		boolean hasFile = homework.answerKeyPdfPath() != null && !homework.answerKeyPdfPath().isBlank();
		return new HomeworkAnswerKeyResponse(count, hasFile);
	}
}
