-- Phase 12-8: 보고서 기간 프리셋 · 기간·학생 선택 생성
-- uk_report_test_student 는 fk_report_test 가 사용하므로 FK를 먼저 제거한다.

CREATE TABLE report_period_preset (
    preset_id     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id      BIGINT       NOT NULL,
    name          VARCHAR(100) NOT NULL,
    period_start  DATE         NOT NULL,
    period_end    DATE         NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_preset_class FOREIGN KEY (class_id) REFERENCES `class` (class_id)
);

CREATE INDEX idx_report_preset_class ON report_period_preset (class_id, period_start DESC);

ALTER TABLE diligence_report
    DROP FOREIGN KEY fk_report_test;

ALTER TABLE diligence_report
    DROP INDEX uk_report_test_student;

ALTER TABLE diligence_report
    MODIFY test_id BIGINT NULL;

ALTER TABLE diligence_report
    ADD COLUMN period_label VARCHAR(200) NULL AFTER period_end,
    ADD COLUMN period_preset_id BIGINT NULL AFTER period_label,
    ADD COLUMN test_rank INT NULL AFTER test_percentile_rank;

ALTER TABLE diligence_report
    ADD CONSTRAINT fk_report_test FOREIGN KEY (test_id) REFERENCES test (test_id);

ALTER TABLE diligence_report
    ADD CONSTRAINT fk_report_period_preset
        FOREIGN KEY (period_preset_id) REFERENCES report_period_preset (preset_id);

CREATE INDEX idx_diligence_report_period ON diligence_report (class_id, period_start, period_end);
