-- Phase 12-2 보완: 테스트 정오표·채점

CREATE TABLE test_answer_key (
    test_id         BIGINT       NOT NULL,
    question_no     INT          NOT NULL,
    correct_answer  VARCHAR(500) NOT NULL,
    PRIMARY KEY (test_id, question_no),
    CONSTRAINT fk_test_answer_key_test
        FOREIGN KEY (test_id) REFERENCES test (test_id) ON DELETE CASCADE
);

ALTER TABLE test_score ADD COLUMN answers JSON NULL;
ALTER TABLE test_score ADD COLUMN correct_count INT NULL;
ALTER TABLE test_score ADD COLUMN graded_at TIMESTAMP NULL;
