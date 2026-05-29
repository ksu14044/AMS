-- Phase 7: 성실도 보고서 (테스트 완료 시 구간별 생성)

CREATE TABLE diligence_report (
    report_id            BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id             BIGINT         NOT NULL,
    student_id           BIGINT         NOT NULL,
    test_id              BIGINT         NOT NULL,
    period_start         TIMESTAMP      NOT NULL,
    period_end           TIMESTAMP      NOT NULL,
    homework_submitted   INT            NOT NULL DEFAULT 0,
    homework_total       INT            NOT NULL DEFAULT 0,
    homework_rate        INT            NOT NULL DEFAULT 0,
    homework_grade       CHAR(1)        NOT NULL,
    clinic_attended      INT            NOT NULL DEFAULT 0,
    clinic_total         INT            NOT NULL DEFAULT 0,
    clinic_rate          INT            NOT NULL DEFAULT 0,
    clinic_grade         CHAR(1)        NOT NULL,
    test_raw_score       DECIMAL(6, 2)  NULL,
    test_class_avg       DECIMAL(6, 2)  NULL,
    test_upper_rank_pct  INT            NULL,
    test_percentile_rank INT            NULL,
    test_grade           CHAR(1)        NULL,
    video_certified      INT            NOT NULL DEFAULT 0,
    video_total          INT            NOT NULL DEFAULT 0,
    video_rate           INT            NOT NULL DEFAULT 0,
    video_grade          CHAR(1)        NOT NULL,
    total_score          INT            NOT NULL DEFAULT 0,
    overall_grade        CHAR(1)        NOT NULL,
    teacher_comment      VARCHAR(2000)  NULL,
    pdf_path             VARCHAR(500)   NULL,
    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_class FOREIGN KEY (class_id) REFERENCES `class` (class_id),
    CONSTRAINT fk_report_student FOREIGN KEY (student_id) REFERENCES `user` (user_id),
    CONSTRAINT fk_report_test FOREIGN KEY (test_id) REFERENCES test (test_id),
    CONSTRAINT uk_report_test_student UNIQUE (test_id, student_id)
);

CREATE INDEX idx_diligence_report_class ON diligence_report (class_id, created_at DESC);
CREATE INDEX idx_diligence_report_student ON diligence_report (student_id, created_at DESC);
