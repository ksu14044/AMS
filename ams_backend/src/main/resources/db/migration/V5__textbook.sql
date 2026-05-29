-- Phase 2-3: textbook (반당 1건 — 제목·출판사·진도)

CREATE TABLE textbook (
    class_id       BIGINT       NOT NULL PRIMARY KEY,
    title          VARCHAR(200) NOT NULL,
    publisher      VARCHAR(100),
    progress_note  TEXT,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_textbook_class FOREIGN KEY (class_id) REFERENCES `class` (class_id)
);
