-- Phase 2-1: class_notice (반 공지)

CREATE TABLE class_notice (
    notice_id      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id       BIGINT       NOT NULL,
    title          VARCHAR(200) NOT NULL,
    body           TEXT         NOT NULL,
    attachment_url VARCHAR(500),
    published_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scheduled_at   TIMESTAMP    NULL,
    author_id      BIGINT       NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_notice_class FOREIGN KEY (class_id) REFERENCES `class` (class_id),
    CONSTRAINT fk_notice_author FOREIGN KEY (author_id) REFERENCES `user` (user_id)
);

CREATE INDEX idx_class_notice_class_published ON class_notice (class_id, published_at DESC);
