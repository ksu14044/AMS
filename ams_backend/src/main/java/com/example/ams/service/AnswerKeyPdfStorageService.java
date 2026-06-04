package com.example.ams.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.config.AmsUploadProperties;

import jakarta.annotation.PostConstruct;

@Service
public class AnswerKeyPdfStorageService {

	private static final Logger log = LoggerFactory.getLogger(AnswerKeyPdfStorageService.class);

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
			"pdf", "png", "jpg", "jpeg", "doc", "docx", "hwp", "hwpx");

	private final Path uploadRoot;
	private final long maxBytes;

	public AnswerKeyPdfStorageService(AmsUploadProperties properties) {
		this.uploadRoot = Path.of(properties.dir()).toAbsolutePath().normalize();
		this.maxBytes = properties.maxSizeMb() * 1024L * 1024L;
	}

	@PostConstruct
	void ensureUploadRoot() {
		try {
			Files.createDirectories(uploadRoot);
			if (!Files.isWritable(uploadRoot)) {
				log.error("업로드 디렉터리에 쓸 수 없습니다: {}", uploadRoot);
			}
		} catch (IOException ex) {
			log.error("업로드 디렉터리를 만들 수 없습니다: {}", uploadRoot, ex);
		}
	}

	public String storeHomeworkAnswerKey(long academyId, long classId, long homeworkId, MultipartFile file) {
		return store("homework", academyId, classId, homeworkId, file);
	}

	public String storeTestAnswerKey(long academyId, long classId, long testId, MultipartFile file) {
		return store("test", academyId, classId, testId, file);
	}

	private String store(String kind, long academyId, long classId, long entityId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "정답지 파일이 필요합니다.");
		}
		if (file.getSize() > maxBytes) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "파일 크기는 10MB 이하여야 합니다.");
		}
		String extension = resolveExtension(file);
		byte[] bytes;
		try {
			bytes = file.getBytes();
		} catch (IOException ex) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "파일을 읽을 수 없습니다.");
		}
		if (bytes.length == 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "빈 파일은 업로드할 수 없습니다.");
		}
		String relative = "answer-keys/" + academyId + "/" + classId + "/" + kind + "-" + entityId + "." + extension;
		Path target = uploadRoot.resolve(relative).normalize();
		if (!target.startsWith(uploadRoot)) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "파일 경로가 올바르지 않습니다.");
		}
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, bytes);
		} catch (IOException ex) {
			log.error("정답지 저장 실패 path={}", target, ex);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "정답지 저장에 실패했습니다. 업로드 경로 권한을 확인하세요.");
		}
		return relative;
	}

	static String resolveExtension(MultipartFile file) {
		String fromName = extensionFromFilename(file.getOriginalFilename());
		if (fromName != null && ALLOWED_EXTENSIONS.contains(fromName)) {
			return fromName;
		}
		String fromType = extensionFromContentType(file.getContentType());
		if (fromType != null && ALLOWED_EXTENSIONS.contains(fromType)) {
			return fromType;
		}
		throw new BusinessException(
				ErrorCode.INVALID_REQUEST,
				"허용되지 않은 파일 형식입니다. pdf, png, jpg, jpeg, doc, docx, hwp, hwpx 만 업로드할 수 있습니다.");
	}

	private static String extensionFromFilename(String filename) {
		if (filename == null || filename.isBlank()) {
			return null;
		}
		int dot = filename.lastIndexOf('.');
		if (dot < 0 || dot == filename.length() - 1) {
			return null;
		}
		return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
	}

	private static String extensionFromContentType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return null;
		}
		return switch (contentType.toLowerCase(Locale.ROOT)) {
			case "application/pdf" -> "pdf";
			case "image/png" -> "png";
			case "image/jpeg", "image/jpg" -> "jpg";
			case "application/msword" -> "doc";
			case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
			case "application/x-hwp", "application/haansofthwp", "application/vnd.hancom.hwp" -> "hwp";
			case "application/vnd.hancom.hwpx", "application/hwp+zip" -> "hwpx";
			default -> null;
		};
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

	public MediaType mediaTypeForPath(String relativePath) {
		String ext = extensionFromFilename(Path.of(relativePath).getFileName().toString());
		if (ext == null) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
		return switch (ext) {
			case "pdf" -> MediaType.APPLICATION_PDF;
			case "png" -> MediaType.IMAGE_PNG;
			case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
			case "doc" -> MediaType.parseMediaType("application/msword");
			case "docx" -> MediaType.parseMediaType(
					"application/vnd.openxmlformats-officedocument.wordprocessingml.document");
			case "hwp" -> MediaType.parseMediaType("application/x-hwp");
			case "hwpx" -> MediaType.parseMediaType("application/vnd.hancom.hwpx");
			default -> MediaType.APPLICATION_OCTET_STREAM;
		};
	}

	public String downloadFilename(String relativePath, String fallbackBaseName) {
		String name = Path.of(relativePath).getFileName().toString();
		if (name == null || name.isBlank()) {
			return fallbackBaseName + ".pdf";
		}
		return name;
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
