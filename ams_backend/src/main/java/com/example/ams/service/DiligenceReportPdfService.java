package com.example.ams.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

import com.example.ams.config.AmsUploadProperties;
import com.example.ams.domain.report.DiligenceReport;

@Service
public class DiligenceReportPdfService {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
	private static final DecimalFormat SCORE_FMT = new DecimalFormat("#.##");

	private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
	private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
	private static final float MARGIN = 42f;
	private static final float CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2);

	private static final float FONT_SIZE_TITLE = 18f;
	private static final float FONT_SIZE_SUB = 11f;
	private static final float FONT_SIZE_BODY = 10f;
	private static final float FONT_SIZE_LABEL = 9f;
	private static final float FONT_SIZE_COMMENT = 10f;

	private static final float LINE_GAP = 4f;
	private static final float SECTION_GAP = 12f;
	private static final float CARD_HEIGHT = 62f;

	private static final List<Path> KOREAN_FONT_CANDIDATES = List.of(
			Path.of("C:/Windows/Fonts/malgun.ttf"),
			Path.of("C:/Windows/Fonts/malgunbd.ttf"),
			Path.of("C:/Windows/Fonts/NanumGothic.ttf"));

	private final AmsUploadProperties uploadProperties;

	public DiligenceReportPdfService(AmsUploadProperties uploadProperties) {
		this.uploadProperties = uploadProperties;
	}

	public String writePdf(
			DiligenceReport report,
			String className,
			String studentName,
			String testTitle) throws IOException {
		Path dir = Path.of(uploadProperties.dir(), "reports");
		Files.createDirectories(dir);
		Path file = dir.resolve("report-" + report.reportId() + ".pdf");

		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);
			PDFont regular = loadKoreanFont(document);
			PDFont bold = regular;

			try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
				float y = PAGE_HEIGHT - MARGIN;

				// Header
				drawText(stream, bold, FONT_SIZE_TITLE, "AMS 성실도 보고서", MARGIN, y);
				y -= FONT_SIZE_TITLE + 6;
				drawText(stream, regular, FONT_SIZE_SUB, className + "  |  " + studentName, MARGIN, y);
				y -= FONT_SIZE_SUB + 2;
				drawText(stream, regular, FONT_SIZE_SUB, "시험: " + nullSafe(testTitle), MARGIN, y);
				y -= FONT_SIZE_SUB + 2;
				drawText(
						stream,
						regular,
						FONT_SIZE_SUB,
						"보고서 기간: "
								+ DATE_FMT.format(report.periodStart().atZone(SEOUL))
								+ " ~ "
								+ DATE_FMT.format(report.periodEnd().atZone(SEOUL)),
						MARGIN,
						y);
				y -= FONT_SIZE_SUB + SECTION_GAP;

				// Summary
				stream.setStrokingColor(0.86f, 0.86f, 0.86f);
				stream.addRect(MARGIN, y - 52, CONTENT_WIDTH, 52);
				stream.stroke();
				drawText(stream, bold, FONT_SIZE_LABEL, "종합 성실도", MARGIN + 10, y - 16);
				drawText(
						stream,
						bold,
						16f,
						report.totalScore() + "점  (" + nullSafe(report.overallGrade()) + "등급)",
						MARGIN + 10,
						y - 36);
				y -= 52 + SECTION_GAP;

				// Metric cards
				y = drawMetricCard(stream, regular, bold, y, "숙제",
						report.homeworkSubmitted() + " / " + report.homeworkTotal() + "건",
						formatRateGrade(report.homeworkRate(), report.homeworkGrade()));
				y = drawMetricCard(stream, regular, bold, y, "클리닉",
						report.clinicAttended() + " / " + report.clinicTotal() + "회",
						formatRateGrade(report.clinicRate(), report.clinicGrade()));
				y = drawMetricCard(stream, regular, bold, y, "테스트",
						formatTestScoreLine(report),
						formatTestRankLine(report));
				y = drawMetricCard(stream, regular, bold, y, "영상 인증",
						report.videoCertified() + " / " + report.videoTotal() + "편",
						formatRateGrade(report.videoRate(), report.videoGrade()));

				// Comment
				if (report.teacherComment() != null && !report.teacherComment().isBlank()) {
					y -= 4;
					stream.setStrokingColor(0.86f, 0.86f, 0.86f);
					float commentHeight = 92f;
					stream.addRect(MARGIN, y - commentHeight, CONTENT_WIDTH, commentHeight);
					stream.stroke();
					drawText(stream, bold, FONT_SIZE_LABEL, "담임 코멘트", MARGIN + 10, y - 14);
					drawWrappedText(
							stream,
							regular,
							FONT_SIZE_COMMENT,
							report.teacherComment(),
							MARGIN + 10,
							y - 30,
							CONTENT_WIDTH - 20,
							FONT_SIZE_COMMENT + LINE_GAP,
							4);
				}
			}
			document.save(file.toFile());
		}

		return "/api/v1/reports/" + report.reportId() + "/pdf";
	}

	private float drawMetricCard(
			PDPageContentStream stream,
			PDFont regular,
			PDFont bold,
			float y,
			String title,
			String line1,
			String line2) throws IOException {
		stream.setStrokingColor(0.90f, 0.90f, 0.90f);
		stream.addRect(MARGIN, y - CARD_HEIGHT, CONTENT_WIDTH, CARD_HEIGHT);
		stream.stroke();
		drawText(stream, bold, FONT_SIZE_LABEL, title, MARGIN + 10, y - 14);
		drawText(stream, regular, FONT_SIZE_BODY, line1, MARGIN + 10, y - 33);
		drawText(stream, regular, FONT_SIZE_BODY, line2, MARGIN + 10, y - 48);
		return y - CARD_HEIGHT - 8;
	}

	private static String formatRateGrade(Integer rate, String grade) {
		if (rate == null) {
			return "해당 기간 데이터 없음";
		}
		return "달성률 " + rate + "%  |  등급 " + nullSafe(grade);
	}

	private static String formatTestScoreLine(DiligenceReport report) {
		if (report.testRawScore() == null) {
			return "응시 기록 없음";
		}
		return "점수 " + SCORE_FMT.format(report.testRawScore()) + "점"
				+ "  |  반 평균 " + (report.testClassAvg() != null ? SCORE_FMT.format(report.testClassAvg()) : "-");
	}

	private static String formatTestRankLine(DiligenceReport report) {
		if (report.testRawScore() == null) {
			return "등급 -";
		}
		if (report.testRank() != null) {
			return report.testRank() + "등  |  등급 " + nullSafe(report.testGrade());
		}
		return "상위 " + (report.testUpperRankPct() != null ? report.testUpperRankPct() : "-")
				+ "%  |  등급 " + nullSafe(report.testGrade());
	}

	private static String nullSafe(String value) {
		return value == null || value.isBlank() ? "-" : value;
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

	private static void drawWrappedText(
			PDPageContentStream stream,
			PDFont font,
			float size,
			String text,
			float x,
			float startY,
			float maxWidth,
			float lineHeight,
			int maxLines) throws IOException {
		if (text == null || text.isBlank()) {
			return;
		}
		List<String> words = List.of(text.trim().split("\\s+"));
		StringBuilder line = new StringBuilder();
		float y = startY;
		int lines = 0;
		for (String word : words) {
			String candidate = line.isEmpty() ? word : line + " " + word;
			float width = font.getStringWidth(candidate) / 1000f * size;
			if (width <= maxWidth) {
				line.setLength(0);
				line.append(candidate);
				continue;
			}
			drawText(stream, font, size, line.toString(), x, y);
			lines++;
			if (lines >= maxLines) {
				return;
			}
			y -= lineHeight;
			line.setLength(0);
			line.append(word);
		}
		if (!line.isEmpty() && lines < maxLines) {
			drawText(stream, font, size, line.toString(), x, y);
		}
	}

	private static PDFont loadKoreanFont(PDDocument document) throws IOException {
		for (Path candidate : KOREAN_FONT_CANDIDATES) {
			if (Files.exists(candidate)) {
				return PDType0Font.load(document, candidate.toFile());
			}
		}
		throw new IOException("한글 폰트를 찾을 수 없습니다. Windows Fonts 경로를 확인하세요.");
	}
}
