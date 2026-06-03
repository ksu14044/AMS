package com.example.ams.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;

final class PlainTextPdfWriter {

	private static final float FONT_SIZE = 10f;
	private static final float LINE_HEIGHT = 14f;
	private static final float MARGIN = 42f;
	private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
	private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
	private static final float MAX_WIDTH = PAGE_WIDTH - MARGIN * 2;

	private static final List<Path> KOREAN_FONT_CANDIDATES = List.of(
			Path.of("C:/Windows/Fonts/malgun.ttf"),
			Path.of("C:/Windows/Fonts/malgunbd.ttf"),
			Path.of("C:/Windows/Fonts/NanumGothic.ttf"),
			Path.of("/usr/share/fonts/truetype/nanum/NanumGothic.ttf"),
			Path.of("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"));

	private PlainTextPdfWriter() {
	}

	static byte[] write(String text) {
		if (text == null || text.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "문서에서 텍스트를 추출할 수 없습니다.");
		}
		try (PDDocument document = new PDDocument();
				ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			PDFont font = loadKoreanFont(document);
			String safeText = sanitizeForPdf(text, font);
			float y = PAGE_HEIGHT - MARGIN;
			PDPage page = newPage(document);
			PDPageContentStream stream = new PDPageContentStream(document, page);

			for (String paragraph : safeText.split("\\r?\\n", -1)) {
				for (String wrapped : wrapLine(paragraph, font, FONT_SIZE)) {
					if (y < MARGIN + LINE_HEIGHT) {
						stream.close();
						page = newPage(document);
						stream = new PDPageContentStream(document, page);
						y = PAGE_HEIGHT - MARGIN;
					}
					drawText(stream, font, FONT_SIZE, wrapped, MARGIN, y);
					y -= LINE_HEIGHT;
				}
			}
			stream.close();
			document.save(out);
			return out.toByteArray();
		} catch (IOException | IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "PDF 변환에 실패했습니다.");
		}
	}

	/** 맑은 고딕 등에 글리프가 없는 특수문자(✓ 등)를 PDF 안전 문자로 치환 */
	private static String sanitizeForPdf(String text, PDFont font) {
		StringBuilder sb = new StringBuilder(text.length());
		for (int offset = 0; offset < text.length();) {
			int codePoint = text.codePointAt(offset);
			if (codePoint == '\t' || (Character.isISOControl(codePoint) && codePoint != '\n' && codePoint != '\r')) {
				sb.append(' ');
				offset += Character.charCount(codePoint);
				continue;
			}
			String glyph = new String(Character.toChars(codePoint));
			if (canEncode(font, glyph)) {
				sb.appendCodePoint(codePoint);
			} else {
				sb.append(replacementFor(codePoint));
			}
			offset += Character.charCount(codePoint);
		}
		return sb.toString();
	}

	private static boolean canEncode(PDFont font, String glyph) {
		try {
			font.encode(glyph);
			return true;
		} catch (IOException | IllegalArgumentException ex) {
			return false;
		}
	}

	private static String replacementFor(int codePoint) {
		return switch (codePoint) {
			case 0x2713, 0x2714 -> "v";
			case 0x2717, 0x2718 -> "X";
			case 0x2022, 0x25CF, 0x25CB, 0x25AA, 0x25AB -> "-";
			case 0x2013, 0x2014 -> "-";
			case 0x2018, 0x2019, 0x201A, 0x201B -> "'";
			case 0x201C, 0x201D, 0x201E, 0x201F -> "\"";
			case 0x00A0 -> " ";
			default -> "?";
		};
	}

	private static PDPage newPage(PDDocument document) {
		PDPage page = new PDPage(PDRectangle.A4);
		document.addPage(page);
		return page;
	}

	private static List<String> wrapLine(String line, PDFont font, float size) throws IOException {
		if (line == null || line.isEmpty()) {
			return List.of("");
		}
		java.util.ArrayList<String> chunks = new java.util.ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (int i = 0; i < line.length(); i++) {
			current.append(line.charAt(i));
			if (textWidth(font, size, current.toString()) > MAX_WIDTH) {
				if (current.length() == 1) {
					chunks.add(current.toString());
					current.setLength(0);
					continue;
				}
				char last = current.charAt(current.length() - 1);
				current.deleteCharAt(current.length() - 1);
				chunks.add(current.toString());
				current.setLength(0);
				current.append(last);
			}
		}
		if (!current.isEmpty()) {
			chunks.add(current.toString());
		}
		return chunks;
	}

	private static float textWidth(PDFont font, float size, String text) throws IOException {
		return font.getStringWidth(text) / 1000f * size;
	}

	private static void drawText(
			PDPageContentStream stream,
			PDFont font,
			float size,
			String text,
			float x,
			float y) throws IOException {
		stream.beginText();
		stream.setFont(font, size);
		stream.newLineAtOffset(x, y);
		stream.showText(text);
		stream.endText();
	}

	private static PDFont loadKoreanFont(PDDocument document) throws IOException {
		for (Path candidate : KOREAN_FONT_CANDIDATES) {
			if (Files.exists(candidate)) {
				return PDType0Font.load(document, candidate.toFile());
			}
		}
		throw new IOException("한글 폰트를 찾을 수 없습니다.");
	}
}
