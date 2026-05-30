-- Phase 12-2: 숙제 정오표·문항 수 · due_at 제거

ALTER TABLE homework ADD COLUMN question_count INT NULL;

CREATE TABLE homework_answer_key (
    homework_id     BIGINT       NOT NULL,
    question_no     INT          NOT NULL,
    correct_answer  VARCHAR(500) NOT NULL,
    PRIMARY KEY (homework_id, question_no),
    CONSTRAINT fk_homework_answer_key_homework
        FOREIGN KEY (homework_id) REFERENCES homework (homework_id) ON DELETE CASCADE
);

ALTER TABLE homework_submission ADD COLUMN answers JSON NULL;
ALTER TABLE homework_submission ADD COLUMN correct_count INT NULL;
ALTER TABLE homework_submission ADD COLUMN completed_at TIMESTAMP NULL;

-- fk_homework_class(class_id)가 idx_homework_class에 의존 → 새 인덱스를 먼저 생성
CREATE INDEX idx_homework_class_v3 ON homework (class_id, created_at DESC);

DROP INDEX idx_homework_class ON homework;

ALTER TABLE homework DROP COLUMN due_at;

-- H2는 RENAME INDEX 미지원 → idx_homework_class_v3 유지 (class_id FK 인덱스 역할 동일)
