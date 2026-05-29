-- Phase 8: notification + device_token (FCM)

CREATE TABLE notification (
    notification_id BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    academy_id      BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    type            VARCHAR(32)  NOT NULL,
    title           VARCHAR(200) NOT NULL,
    body            VARCHAR(500) NOT NULL,
    reference_type  VARCHAR(32),
    reference_id    BIGINT,
    read_at         TIMESTAMP    NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_academy FOREIGN KEY (academy_id) REFERENCES academy (academy_id),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES `user` (user_id)
);

CREATE INDEX idx_notification_user_created ON notification (user_id, created_at DESC);
CREATE INDEX idx_notification_user_unread ON notification (user_id, read_at, created_at DESC);

CREATE TABLE device_token (
    device_token_id BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    fcm_token       VARCHAR(512) NOT NULL,
    platform        VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_token_user FOREIGN KEY (user_id) REFERENCES `user` (user_id),
    CONSTRAINT uk_device_token_user_token UNIQUE (user_id, fcm_token)
);

CREATE INDEX idx_device_token_user ON device_token (user_id);
