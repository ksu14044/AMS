-- Phase 5: clinic_week, clinic_reservation (예약·잠금)

CREATE TABLE clinic_week (
    class_id          BIGINT      NOT NULL,
    week_start_date   DATE        NOT NULL,
    status            VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    locked_at         TIMESTAMP   NULL,
    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (class_id, week_start_date),
    CONSTRAINT fk_clinic_week_class FOREIGN KEY (class_id) REFERENCES `class` (class_id),
    CONSTRAINT chk_clinic_week_status CHECK (status IN ('OPEN', 'LOCKED'))
);

CREATE TABLE clinic_reservation (
    reservation_id BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    slot_id        BIGINT       NOT NULL,
    student_id     BIGINT       NOT NULL,
    status         VARCHAR(16)  NOT NULL DEFAULT 'RESERVED',
    result_attended BOOLEAN,
    result_memo    VARCHAR(500),
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservation_slot FOREIGN KEY (slot_id) REFERENCES clinic_slot (slot_id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_student FOREIGN KEY (student_id) REFERENCES `user` (user_id),
    CONSTRAINT uk_reservation_slot_student UNIQUE (slot_id, student_id),
    CONSTRAINT chk_reservation_status CHECK (status IN ('RESERVED', 'CONFIRMED'))
);

CREATE INDEX idx_clinic_reservation_slot ON clinic_reservation (slot_id);
