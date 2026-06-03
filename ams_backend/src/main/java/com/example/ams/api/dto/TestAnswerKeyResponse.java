package com.example.ams.api.dto;

import com.example.ams.domain.clazz.TestExam;

public record TestAnswerKeyResponse(
		int questionCount,
		boolean hasAnswerKeyFile) {

	public static TestAnswerKeyResponse from(TestExam test) {
		int count = test.questionCount() != null ? test.questionCount() : 0;
		boolean hasFile = test.answerKeyPdfPath() != null && !test.answerKeyPdfPath().isBlank();
		return new TestAnswerKeyResponse(count, hasFile);
	}
}
