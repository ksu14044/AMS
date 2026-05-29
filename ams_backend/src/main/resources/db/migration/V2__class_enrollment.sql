-- Phase 1: class (homeroom) + class_enrollment (student assignment)

CREATE TABLE `class` (
    class_id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    academy_id          BIGINT       NOT NULL,
    subject             VARCHAR(16)  NOT NULL,
    name                VARCHAR(100) NOT NULL,
    homeroom_teacher_id BIGINT       NOT NULL,
    classroom           VARCHAR(50),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_class_academy FOREIGN KEY (academy_id) REFERENCES academy (academy_id),
    CONSTRAINT fk_class_homeroom FOREIGN KEY (homeroom_teacher_id) REFERENCES `user` (user_id),
    CONSTRAINT uk_class_academy_name UNIQUE (academy_id, name)
);

CREATE INDEX idx_class_academy ON `class` (academy_id);

CREATE TABLE class_enrollment (
    enrollment_id BIGINT    NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id      BIGINT    NOT NULL,
    student_id    BIGINT    NOT NULL,
    assigned_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by   BIGINT    NOT NULL,
    CONSTRAINT fk_enrollment_class FOREIGN KEY (class_id) REFERENCES `class` (class_id),
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES `user` (user_id),
    CONSTRAINT fk_enrollment_assigner FOREIGN KEY (assigned_by) REFERENCES `user` (user_id),
    CONSTRAINT uk_enrollment_class_student UNIQUE (class_id, student_id)
);

CREATE INDEX idx_enrollment_student ON class_enrollment (student_id);
