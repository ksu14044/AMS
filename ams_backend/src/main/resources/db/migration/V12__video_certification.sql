-- Phase 4: video_certification (학생 인증사진)

CREATE TABLE video_certification (
    certification_id BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    video_id         BIGINT       NOT NULL,
    student_id       BIGINT       NOT NULL,
    image_url        VARCHAR(500) NOT NULL,
    submitted_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cert_video FOREIGN KEY (video_id) REFERENCES video_lesson (video_id),
    CONSTRAINT fk_cert_student FOREIGN KEY (student_id) REFERENCES `user` (user_id),
    CONSTRAINT uk_cert_video_student UNIQUE (video_id, student_id)
);

CREATE INDEX idx_video_cert_video ON video_certification (video_id);
