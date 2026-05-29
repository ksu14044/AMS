-- Phase 3: assistant_class_assignment (담임이 조교 담당 반 지정)

CREATE TABLE assistant_class_assignment (
    assignment_id BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    assistant_id  BIGINT   NOT NULL,
    class_id      BIGINT   NOT NULL,
    assigned_by   BIGINT   NOT NULL,
    assigned_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assistant_assignment_user FOREIGN KEY (assistant_id) REFERENCES `user` (user_id),
    CONSTRAINT fk_assistant_assignment_class FOREIGN KEY (class_id) REFERENCES `class` (class_id),
    CONSTRAINT fk_assistant_assignment_by FOREIGN KEY (assigned_by) REFERENCES `user` (user_id),
    CONSTRAINT uk_assistant_class UNIQUE (assistant_id, class_id)
);

CREATE INDEX idx_assistant_assignment_class ON assistant_class_assignment (class_id);
CREATE INDEX idx_assistant_assignment_assistant ON assistant_class_assignment (assistant_id);
