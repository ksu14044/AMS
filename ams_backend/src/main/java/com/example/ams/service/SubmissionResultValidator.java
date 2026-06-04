package com.example.ams.service;

import java.util.List;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.common.WrongQuestionNosJson;

public final class SubmissionResultValidator {

	private SubmissionResultValidator() {
	}

	public record Result(int correctCount, List<Integer> wrongQuestionNos) {
	}

	public static Result fromCorrectCount(int questionCount, int correctCount) {
		if (questionCount <= 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "문항 수가 설정되지 않았습니다.");
		}
		if (correctCount < 0 || correctCount > questionCount) {
			throw new BusinessException(
					ErrorCode.INVALID_REQUEST,
					"맞은 문항 수는 0 이상 " + questionCount + " 이하여야 합니다.");
		}
		return new Result(correctCount, List.of());
	}

	public static Result fromWrongQuestions(int questionCount, List<Integer> wrongQuestionNos) {
		if (questionCount <= 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "문항 수가 설정되지 않았습니다.");
		}
		List<Integer> normalized = WrongQuestionNosJson.normalize(wrongQuestionNos, questionCount);
		if (normalized.size() > questionCount) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "틀린 문항이 문항 수를 초과합니다.");
		}
		int correctCount = questionCount - normalized.size();
		return new Result(correctCount, normalized);
	}
}
