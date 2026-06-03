-- 정답지 PDF 업로드 · 틀린 문항 번호 (H2 호환)

ALTER TABLE homework ADD COLUMN answer_key_pdf_path VARCHAR(500) NULL;
ALTER TABLE test ADD COLUMN answer_key_pdf_path VARCHAR(500) NULL;
ALTER TABLE homework_submission ADD COLUMN wrong_question_nos JSON NULL;
ALTER TABLE test_score ADD COLUMN wrong_question_nos JSON NULL;
