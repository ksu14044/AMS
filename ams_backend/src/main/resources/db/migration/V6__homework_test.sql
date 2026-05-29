-- Phase 2-4: homework + test (반 단위 정의, 학생별 결과)

CREATE TABLE homework (
    homework_id BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id    BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    due_at      TIMESTAMP    NOT NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'SCHEDULED',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_homework_class FOREIGN KEY (class_id) REFERENCES `class` (class_id)
);

CREATE INDEX idx_homework_class ON homework (class_id, due_at DESC);

CREATE TABLE homework_submission (
    submission_id BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    homework_id   BIGINT        NOT NULL,
    student_id    BIGINT        NOT NULL,
    submitted     TINYINT(1)    NOT NULL DEFAULT 0,
    submitted_at  TIMESTAMP     NULL,
    score         DECIMAL(6, 2) NULL,
    grade         VARCHAR(16)   NULL,
    memo          VARCHAR(500)  NULL,
    CONSTRAINT fk_submission_homework FOREIGN KEY (homework_id) REFERENCES homework (homework_id),
    CONSTRAINT fk_submission_student FOREIGN KEY (student_id) REFERENCES `user` (user_id),
    CONSTRAINT uk_submission_homework_student UNIQUE (homework_id, student_id)
);

CREATE TABLE test (
    test_id        BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id       BIGINT        NOT NULL,
    title          VARCHAR(200)  NOT NULL,
    test_at        TIMESTAMP     NOT NULL,
    status         VARCHAR(16)   NOT NULL DEFAULT 'SCHEDULED',
    class_average  DECIMAL(6, 2) NULL,
    completed_at   TIMESTAMP     NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_class FOREIGN KEY (class_id) REFERENCES `class` (class_id)
);

CREATE INDEX idx_test_class ON test (class_id, test_at DESC);

CREATE TABLE test_score (
    score_id         BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    test_id          BIGINT        NOT NULL,
    student_id       BIGINT        NOT NULL,
    raw_score        DECIMAL(6, 2) NULL,
    grade            VARCHAR(16)   NULL,
    class_avg        DECIMAL(6, 2) NULL,
    upper_rank_pct   INT           NULL,
    percentile_rank  INT           NULL,
    CONSTRAINT fk_score_test FOREIGN KEY (test_id) REFERENCES test (test_id),
    CONSTRAINT fk_score_student FOREIGN KEY (student_id) REFERENCES `user` (user_id),
    CONSTRAINT uk_score_test_student UNIQUE (test_id, student_id)
);
