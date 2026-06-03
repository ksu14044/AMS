package com.example.ams.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.config.AmsUploadProperties;

@Service
public class AnswerKeyPdfStorageService {

	private final Path uploadRoot;
	private final long maxBytes;

	public AnswerKeyPdfStorageService(AmsUploadProperties properties) {
		this.uploadRoot = Path.of(properties.dir()).toAbsolutePath().normalize();
		this.maxBytes = properties.maxSizeMb() * 1024L * 1024L;
	}

	public String storeHomeworkAnswerKey(long academyId, long classId, long homeworkId, MultipartFile file) {
		return store("homework", academyId, classId, homeworkId, file);
	}

	public String storeTestAnswerKey(long academyId, long classId, long testId, MultipartFile file) {
		return store("test", academyId, classId, testId, file);
	}

	private String store(String kind, long academyId, long classId, long entityId, MultipartFile file) {
		if (file.getSize() > maxBytes) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "파일 크기는 10MB 이하여야 합니다.");
		}
		byte[] pdfBytes = AnswerKeyPdfConverter.toPdf(file);
		if (!AnswerKeyPdfConverter.isValidPdf(pdfBytes)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "유효한 PDF 파일이 아닙니다.");
		}
		String relative = "answer-keys/" + academyId + "/" + classId + "/" + kind + "-" + entityId + ".pdf";
		Path target = uploadRoot.resolve(relative).normalize();
		if (!target.startsWith(uploadRoot)) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "파일 경로가 올바르지 않습니다.");
		}
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, pdfBytes);
		} catch (IOException ex) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "정답지 저장에 실패했습니다.");
		}
		return relative;
	}

	public Resource load(String relativePath) {
		if (relativePath == null || relativePath.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "정답지가 등록되지 않았습니다.");
		}
		Path file = uploadRoot.resolve(relativePath).normalize();
		if (!file.startsWith(uploadRoot) || !Files.isRegularFile(file)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "정답지 파일을 찾을 수 없습니다.");
		}
		return new FileSystemResource(file);
	}

	public void deleteIfExists(String relativePath) {
		if (relativePath == null || relativePath.isBlank()) {
			return;
		}
		Path file = uploadRoot.resolve(relativePath).normalize();
		if (file.startsWith(uploadRoot)) {
			try {
				Files.deleteIfExists(file);
			} catch (IOException ignored) {
				// best effort
			}
		}
	}
}
