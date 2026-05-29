-- Phase 2-2: class_schedule (요일·시간·강의실, 반당 복수 슬롯)

CREATE TABLE class_schedule (
    schedule_id  BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id     BIGINT      NOT NULL,
    day_of_week  VARCHAR(8)  NOT NULL,
    start_time   TIME        NOT NULL,
    end_time     TIME        NOT NULL,
    room         VARCHAR(50),
    CONSTRAINT fk_schedule_class FOREIGN KEY (class_id) REFERENCES `class` (class_id),
    CONSTRAINT uk_schedule_class_day_start UNIQUE (class_id, day_of_week, start_time)
);

CREATE INDEX idx_schedule_class ON class_schedule (class_id);
