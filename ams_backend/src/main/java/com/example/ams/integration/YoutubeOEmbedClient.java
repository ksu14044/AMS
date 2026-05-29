package com.example.ams.integration;

import java.net.URI;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class YoutubeOEmbedClient {

	private final RestClient restClient;

	public YoutubeOEmbedClient(RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.build();
	}

	public Optional<YoutubeOEmbedMetadata> fetch(String youtubeUrl) {
		URI uri = UriComponentsBuilder
				.fromUriString("https://www.youtube.com/oembed")
				.queryParam("url", youtubeUrl)
				.queryParam("format", "json")
				.build()
				.toUri();
		try {
			YoutubeOEmbedMetadata metadata = restClient.get()
					.uri(uri)
					.retrieve()
					.body(YoutubeOEmbedMetadata.class);
			if (metadata == null || metadata.thumbnailUrl() == null || metadata.thumbnailUrl().isBlank()) {
				return Optional.empty();
			}
			return Optional.of(metadata);
		} catch (Exception ex) {
			return Optional.empty();
		}
	}
}
