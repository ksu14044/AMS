-- Phase 2-5: video_lesson (반별 영상 — URL·제목, 썸네일/oEmbed는 Phase 4)

CREATE TABLE video_lesson (
    video_id       BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id       BIGINT       NOT NULL,
    youtube_url    VARCHAR(500) NOT NULL,
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    thumbnail_url  VARCHAR(500),
    published_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    author_id      BIGINT       NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_video_class FOREIGN KEY (class_id) REFERENCES `class` (class_id),
    CONSTRAINT fk_video_author FOREIGN KEY (author_id) REFERENCES `user` (user_id)
);

CREATE INDEX idx_video_lesson_class_published ON video_lesson (class_id, published_at DESC);
