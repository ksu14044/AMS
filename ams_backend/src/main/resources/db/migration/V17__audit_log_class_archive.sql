-- Phase 11: audit log + class archive (soft delete)

CREATE TABLE audit_log (
    audit_id      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    academy_id    BIGINT       NOT NULL,
    actor_user_id BIGINT       NOT NULL,
    action        VARCHAR(64)  NOT NULL,
    entity_type   VARCHAR(32)  NOT NULL,
    entity_id     BIGINT,
    detail_json   JSON,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_academy FOREIGN KEY (academy_id) REFERENCES academy (academy_id),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES `user` (user_id)
);

CREATE INDEX idx_audit_academy_created ON audit_log (academy_id, created_at DESC);

ALTER TABLE `class` ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE `class` ADD COLUMN archived_at TIMESTAMP NULL;

CREATE INDEX idx_class_academy_status ON `class` (academy_id, status);

CREATE TABLE class_record_archive (
    archive_id    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    academy_id    BIGINT       NOT NULL,
    class_id      BIGINT       NOT NULL,
    class_name    VARCHAR(100) NOT NULL,
    subject       VARCHAR(16)  NOT NULL,
    archived_by   BIGINT       NOT NULL,
    archived_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    snapshot_json JSON         NOT NULL,
    CONSTRAINT fk_archive_academy FOREIGN KEY (academy_id) REFERENCES academy (academy_id),
    CONSTRAINT fk_archive_archiver FOREIGN KEY (archived_by) REFERENCES `user` (user_id)
);

CREATE INDEX idx_archive_academy ON class_record_archive (academy_id, archived_at DESC);
