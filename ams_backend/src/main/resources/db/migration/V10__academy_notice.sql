-- Phase 2-7: academy_notice (학원 공지 — 행정·관리자 작성, 학원 소속 전체 열람)

CREATE TABLE academy_notice (
    notice_id      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    academy_id     BIGINT       NOT NULL,
    title          VARCHAR(200) NOT NULL,
    body           TEXT         NOT NULL,
    attachment_url VARCHAR(500),
    published_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    author_id      BIGINT       NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_academy_notice_academy FOREIGN KEY (academy_id) REFERENCES academy (academy_id),
    CONSTRAINT fk_academy_notice_author FOREIGN KEY (author_id) REFERENCES `user` (user_id)
);

CREATE INDEX idx_academy_notice_published ON academy_notice (academy_id, published_at DESC);
