package com.example.ams.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.web.multipart.MultipartFile;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;

public final class AnswerKeyPdfConverter {

	private AnswerKeyPdfConverter() {
	}

	public static byte[] toPdf(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "정답지 파일이 필요합니다.");
		}
		byte[] bytes;
		try {
			bytes = file.getBytes();
		} catch (IOException ex) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "파일을 읽을 수 없습니다.");
		}
		if (bytes.length == 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "빈 파일은 업로드할 수 없습니다.");
		}
		if (isPdf(file, bytes)) {
			return bytes;
		}
		BufferedImage image = readImage(bytes);
		if (image != null) {
			return imageToPdf(image);
		}
		String extension = OfficeDocumentTextExtractor.extensionOf(file);
		if (OfficeDocumentTextExtractor.isOfficeDocument(extension)) {
			String text = OfficeDocumentTextExtractor.extract(file, bytes, extension);
			return PlainTextPdfWriter.write(text);
		}
		throw new BusinessException(
				ErrorCode.INVALID_REQUEST,
				"PDF, 이미지, 워드(doc/docx), 한글(hwp/hwpx) 파일만 업로드할 수 있습니다.");
	}

	private static boolean isPdf(MultipartFile file, byte[] bytes) {
		if (bytes.length >= 4 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F') {
			return true;
		}
		String contentType = file.getContentType();
		if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
			return true;
		}
		String name = file.getOriginalFilename();
		return name != null && name.toLowerCase().endsWith(".pdf");
	}

	private static BufferedImage readImage(byte[] bytes) {
		try {
			return ImageIO.read(new java.io.ByteArrayInputStream(bytes));
		} catch (IOException ex) {
			return null;
		}
	}

	private static byte[] imageToPdf(BufferedImage image) {
		try (PDDocument document = new PDDocument();
				ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);
			PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, toPngBytes(image), "answer-key");
			float pageWidth = page.getMediaBox().getWidth();
			float pageHeight = page.getMediaBox().getHeight();
			float margin = 36f;
			float maxWidth = pageWidth - margin * 2;
			float maxHeight = pageHeight - margin * 2;
			float scale = Math.min(maxWidth / pdImage.getWidth(), maxHeight / pdImage.getHeight());
			float width = pdImage.getWidth() * scale;
			float height = pdImage.getHeight() * scale;
			float x = (pageWidth - width) / 2f;
			float y = (pageHeight - height) / 2f;
			try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
				contentStream.drawImage(pdImage, x, y, width, height);
			}
			document.save(out);
			return out.toByteArray();
		} catch (IOException ex) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "PDF 변환에 실패했습니다.");
		}
	}

	private static byte[] toPngBytes(BufferedImage image) throws IOException {
		ByteArrayOutputStream png = new ByteArrayOutputStream();
		ImageIO.write(image, "png", png);
		return png.toByteArray();
	}

	static boolean isValidPdf(byte[] bytes) {
		try (PDDocument ignored = Loader.loadPDF(bytes)) {
			return true;
		} catch (IOException ex) {
			return false;
		}
	}
}
