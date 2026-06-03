-- Phase 12-9: 학부모–학생 연결 (§19)
-- H2(test, MODE=MySQL) 호환

CREATE TABLE parent_student_link (
    link_id     BIGINT    NOT NULL AUTO_INCREMENT PRIMARY KEY,
    parent_id   BIGINT    NOT NULL,
    student_id  BIGINT    NOT NULL,
    linked_by   BIGINT    NOT NULL,
    linked_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_parent_link_parent FOREIGN KEY (parent_id) REFERENCES `user` (user_id),
    CONSTRAINT fk_parent_link_student FOREIGN KEY (student_id) REFERENCES `user` (user_id),
    CONSTRAINT fk_parent_link_linked_by FOREIGN KEY (linked_by) REFERENCES `user` (user_id),
    CONSTRAINT uk_parent_student UNIQUE (parent_id, student_id)
);

CREATE INDEX idx_parent_link_student ON parent_student_link (student_id);
