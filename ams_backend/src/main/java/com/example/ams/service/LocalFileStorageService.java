package com.example.ams.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.config.AmsUploadProperties;

@Service
public class LocalFileStorageService {

	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/jpg");

	private final Path uploadRoot;
	private final long maxBytes;

	public LocalFileStorageService(AmsUploadProperties properties) {
		this.uploadRoot = Path.of(properties.dir()).toAbsolutePath().normalize();
		this.maxBytes = properties.maxSizeMb() * 1024L * 1024L;
	}

	public String storeCertificationImage(long academyId, long studentId, MultipartFile file) {
		validateImage(file);
		String extension = extensionFor(file.getContentType());
		String date = LocalDate.now().toString();
		String filename = UUID.randomUUID() + extension;
		Path targetDir = uploadRoot.resolve(String.valueOf(academyId))
				.resolve(String.valueOf(studentId))
				.resolve(date);
		try {
			Files.createDirectories(targetDir);
			Path target = targetDir.resolve(filename);
			file.transferTo(target);
			return "/api/v1/media/" + academyId + "/" + studentId + "/" + date + "/" + filename;
		} catch (IOException ex) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "파일 저장에 실패했습니다.");
		}
	}

	private void validateImage(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미지 파일이 필요합니다.");
		}
		if (file.getSize() > maxBytes) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "파일 크기는 10MB 이하여야 합니다.");
		}
		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "JPEG 또는 PNG만 업로드할 수 있습니다.");
		}
	}

	private String extensionFor(String contentType) {
		if (contentType != null && contentType.toLowerCase().contains("png")) {
			return ".png";
		}
		return ".jpg";
	}

	public Path resolveMediaPath(String academyId, String studentId, String date, String filename) {
		return uploadRoot.resolve(academyId).resolve(studentId).resolve(date).resolve(filename).normalize();
	}
}
