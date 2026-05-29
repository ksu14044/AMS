package com.example.ams.common;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * Phase 2-5: URL 저장만 검증. oEmbed·썸네일 자동 추출은 Phase 4.
 */
public final class YoutubeUrlValidator {

	private static final Pattern YOUTUBE_HOST = Pattern.compile(
			"^(www\\.)?(youtube\\.com|youtu\\.be|m\\.youtube\\.com)$",
			Pattern.CASE_INSENSITIVE);

	private YoutubeUrlValidator() {
	}

	public static void requireValid(String url) {
		if (url == null || url.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		URI uri;
		try {
			uri = URI.create(url.trim());
		} catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		if (uri.getHost() == null || !YOUTUBE_HOST.matcher(uri.getHost()).matches()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
	}
}
