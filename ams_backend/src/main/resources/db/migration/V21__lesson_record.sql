-- Phase 12-1: 수업기록 (lesson_record) + 하위 항목 FK (nullable, 기존 데이터 유지)

CREATE TABLE lesson_record (
    lesson_record_id BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id         BIGINT       NOT NULL,
    lesson_date      DATE         NOT NULL,
    summary          TEXT         NOT NULL,
    author_id        BIGINT       NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lesson_record_class FOREIGN KEY (class_id) REFERENCES `class` (class_id),
    CONSTRAINT fk_lesson_record_author FOREIGN KEY (author_id) REFERENCES `user` (user_id),
    CONSTRAINT uk_lesson_record_class_date UNIQUE (class_id, lesson_date)
);

CREATE INDEX idx_lesson_record_class ON lesson_record (class_id, lesson_date DESC);

ALTER TABLE homework ADD COLUMN lesson_record_id BIGINT NULL;
ALTER TABLE homework ADD CONSTRAINT fk_homework_lesson_record
    FOREIGN KEY (lesson_record_id) REFERENCES lesson_record (lesson_record_id);

ALTER TABLE test ADD COLUMN lesson_record_id BIGINT NULL;
ALTER TABLE test ADD CONSTRAINT fk_test_lesson_record
    FOREIGN KEY (lesson_record_id) REFERENCES lesson_record (lesson_record_id);

ALTER TABLE video_lesson ADD COLUMN lesson_record_id BIGINT NULL;
ALTER TABLE video_lesson ADD CONSTRAINT fk_video_lesson_record
    FOREIGN KEY (lesson_record_id) REFERENCES lesson_record (lesson_record_id);

ALTER TABLE clinic_slot ADD COLUMN lesson_record_id BIGINT NULL;
ALTER TABLE clinic_slot ADD CONSTRAINT fk_clinic_slot_lesson_record
    FOREIGN KEY (lesson_record_id) REFERENCES lesson_record (lesson_record_id);
