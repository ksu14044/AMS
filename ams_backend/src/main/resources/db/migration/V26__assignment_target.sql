-- Phase 12-4: 대상 학생 지정

CREATE TABLE assignment_target (
    entity_type  VARCHAR(32)  NOT NULL,
    entity_id    BIGINT       NOT NULL,
    student_id   BIGINT       NOT NULL,
    PRIMARY KEY (entity_type, entity_id, student_id),
    CONSTRAINT fk_assignment_target_student
        FOREIGN KEY (student_id) REFERENCES `user` (user_id)
);

CREATE INDEX idx_assignment_target_entity ON assignment_target (entity_type, entity_id);
