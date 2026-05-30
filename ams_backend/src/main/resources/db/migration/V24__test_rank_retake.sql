-- Phase 12-3: 테스트 석차·재시험

ALTER TABLE test ADD COLUMN question_count INT NULL;
ALTER TABLE test ADD COLUMN retake_threshold_count INT NULL;
ALTER TABLE test ADD COLUMN parent_test_id BIGINT NULL;
ALTER TABLE test ADD COLUMN retake_attempt_no INT NOT NULL DEFAULT 0;

ALTER TABLE test ADD CONSTRAINT fk_test_parent
    FOREIGN KEY (parent_test_id) REFERENCES test (test_id);

ALTER TABLE test_score ADD COLUMN `rank` INT NULL;
