package com.example.ams.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YoutubeOEmbedMetadata(
		String title,
		@JsonProperty("thumbnail_url") String thumbnailUrl) {
}
