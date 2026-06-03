package com.example.ams.api.dto;

import java.util.List;

import jakarta.validation.constraints.Min;

public record RecordSubmissionResultRequest(List<@Min(1) Integer> wrongQuestionNos) {

	public RecordSubmissionResultRequest {
		if (wrongQuestionNos == null) {
			wrongQuestionNos = List.of();
		}
	}
}
