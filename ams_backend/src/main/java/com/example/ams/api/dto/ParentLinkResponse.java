package com.example.ams.api.dto;

import java.time.Instant;

public record ParentLinkResponse(
		long linkId,
		long parentId,
		String parentName,
		String parentEmail,
		Instant linkedAt) {
}
