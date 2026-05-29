-- Phase 2-6: clinic_slot (주차·요일·시간·조교 — 예약은 Phase 5)

CREATE TABLE clinic_slot (
    slot_id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id         BIGINT      NOT NULL,
    week_start_date  DATE        NOT NULL,
    day_of_week      VARCHAR(8)  NOT NULL,
    start_time       TIME        NOT NULL,
    assistant_id     BIGINT,
    max_capacity     INT         NOT NULL DEFAULT 1,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_clinic_slot_class FOREIGN KEY (class_id) REFERENCES `class` (class_id),
    CONSTRAINT fk_clinic_slot_assistant FOREIGN KEY (assistant_id) REFERENCES `user` (user_id),
    CONSTRAINT uk_clinic_slot UNIQUE (class_id, week_start_date, day_of_week, start_time),
    CONSTRAINT chk_clinic_day CHECK (day_of_week IN ('MON', 'TUE', 'WED', 'THU', 'FRI')),
    CONSTRAINT chk_clinic_capacity CHECK (max_capacity >= 1 AND max_capacity <= 20)
);

CREATE INDEX idx_clinic_slot_class_week ON clinic_slot (class_id, week_start_date);
