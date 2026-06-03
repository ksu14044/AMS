package com.example.ams.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.web.multipart.MultipartFile;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwplib.tool.textextractor.TextExtractMethod;
import kr.dogfoot.hwplib.tool.textextractor.TextExtractor;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.tool.textextractor.TextMarks;

final class OfficeDocumentTextExtractor {

	private OfficeDocumentTextExtractor() {
	}

	static String extract(MultipartFile file, byte[] bytes, String extension) {
		return switch (extension) {
			case "docx" -> extractDocx(bytes);
			case "doc" -> extractDoc(bytes);
			case "hwp" -> extractHwp(bytes);
			case "hwpx" -> extractHwpx(bytes);
			default -> throw new BusinessException(
					ErrorCode.INVALID_REQUEST,
					"지원하지 않는 문서 형식입니다: " + extension);
		};
	}

	static String extensionOf(MultipartFile file) {
		String name = file.getOriginalFilename();
		if (name == null) {
			return "";
		}
		int dot = name.lastIndexOf('.');
		if (dot < 0 || dot == name.length() - 1) {
			return "";
		}
		return name.substring(dot + 1).toLowerCase();
	}

	static boolean isOfficeDocument(String extension) {
		return switch (extension) {
			case "doc", "docx", "hwp", "hwpx" -> true;
			default -> false;
		};
	}

	private static String extractDocx(byte[] bytes) {
		try (InputStream in = new ByteArrayInputStream(bytes);
				XWPFDocument document = new XWPFDocument(in)) {
			StringBuilder sb = new StringBuilder();
			for (XWPFParagraph paragraph : document.getParagraphs()) {
				appendLine(sb, paragraph.getText());
			}
			for (XWPFTable table : document.getTables()) {
				for (XWPFTableRow row : table.getRows()) {
					for (XWPFTableCell cell : row.getTableCells()) {
						for (XWPFParagraph paragraph : cell.getParagraphs()) {
							appendLine(sb, paragraph.getText());
						}
					}
				}
			}
			return sb.toString().trim();
		} catch (IOException | RuntimeException ex) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "워드(docx) 파일을 읽을 수 없습니다.");
		}
	}

	private static String extractDoc(byte[] bytes) {
		try (InputStream in = new ByteArrayInputStream(bytes);
				HWPFDocument document = new HWPFDocument(in);
				WordExtractor extractor = new WordExtractor(document)) {
			return extractor.getText().trim();
		} catch (IOException | RuntimeException ex) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "워드(doc) 파일을 읽을 수 없습니다.");
		}
	}

	private static String extractHwp(byte[] bytes) {
		try {
			HWPFile hwpFile = HWPReader.fromInputStream(new ByteArrayInputStream(bytes));
			return TextExtractor.extract(hwpFile, TextExtractMethod.InsertControlTextBetweenParagraphText).trim();
		} catch (IOException | RuntimeException ex) {
			throw new BusinessException(
					ErrorCode.INVALID_REQUEST,
					"한글(hwp) 파일을 읽을 수 없습니다. PDF로 저장 후 업로드해 주세요.");
		}
	}

	private static String extractHwpx(byte[] bytes) {
		Path temp = null;
		try {
			temp = Files.createTempFile("ams-answer-key-", ".hwpx");
			Files.write(temp, bytes);
			HWPXFile hwpxFile = HWPXReader.fromFile(temp.toFile());
			TextMarks textMarks = new TextMarks();
			return kr.dogfoot.hwpxlib.tool.textextractor.TextExtractor.extract(
					hwpxFile,
					kr.dogfoot.hwpxlib.tool.textextractor.TextExtractMethod.InsertControlTextBetweenParagraphText,
					true,
					textMarks).trim();
		} catch (Exception ex) {
			throw new BusinessException(
					ErrorCode.INVALID_REQUEST,
					"한글(hwpx) 파일을 읽을 수 없습니다. PDF로 저장 후 업로드해 주세요.");
		} finally {
			if (temp != null) {
				try {
					Files.deleteIfExists(temp);
				} catch (IOException ignored) {
					// best effort
				}
			}
		}
	}

	private static void appendLine(StringBuilder sb, String line) {
		if (line == null || line.isBlank()) {
			return;
		}
		if (!sb.isEmpty()) {
			sb.append('\n');
		}
		sb.append(line.trim());
	}
}
