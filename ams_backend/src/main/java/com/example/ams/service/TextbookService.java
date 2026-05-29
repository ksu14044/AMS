package com.example.ams.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.clazz.Textbook;
import com.example.ams.repository.TextbookRepository;

@Service
public class TextbookService {

	private final TextbookRepository textbookRepository;
	private final ClassAccessService classAccessService;

	public TextbookService(TextbookRepository textbookRepository, ClassAccessService classAccessService) {
		this.textbookRepository = textbookRepository;
		this.classAccessService = classAccessService;
	}

	public Optional<Textbook> getTextbook(long classId) {
		classAccessService.requireReadableClass(classId);
		return textbookRepository.findByClassId(classId);
	}

	@Transactional
	public Textbook updateTextbook(long classId, String title, String publisher, String progressNote) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		return textbookRepository.upsert(classId, title, publisher, progressNote);
	}
}
