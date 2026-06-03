package com.example.ams.domain.link;

import java.time.Instant;

public record ParentStudentLink(
		long linkId,
		long parentId,
		long studentId,
		long linkedBy,
		Instant linkedAt) {
}
