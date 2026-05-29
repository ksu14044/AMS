package com.example.ams.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class StudyRecordGradesTest {

	@Test
	void rawScorePercent_clampsAndRounds() {
		assertEquals(0, StudyRecordGrades.rawScorePercent(null));
		assertEquals(90, StudyRecordGrades.rawScorePercent(new BigDecimal("89.6")));
		assertEquals(100, StudyRecordGrades.rawScorePercent(new BigDecimal("150")));
		assertEquals(0, StudyRecordGrades.rawScorePercent(new BigDecimal("-5")));
	}

	@Test
	void weightedTotalPercent_usesRawScoreForTest() {
		assertEquals(97, StudyRecordGrades.weightedTotalPercent(100, 100, 90, true));
	}
}
