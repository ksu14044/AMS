-- Phase 0: academy (tenant) + user (auth, RBAC role on row)

CREATE TABLE academy (
    academy_id   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    code         VARCHAR(32)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_academy_code UNIQUE (code)
);

CREATE TABLE `user` (
    user_id       BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    academy_id    BIGINT       NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    role          VARCHAR(32)  NOT NULL,
    subject       VARCHAR(16),
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_academy FOREIGN KEY (academy_id) REFERENCES academy (academy_id),
    CONSTRAINT uk_user_academy_email UNIQUE (academy_id, email)
);

CREATE INDEX idx_user_academy_status ON `user` (academy_id, status);
CREATE INDEX idx_user_academy_role ON `user` (academy_id, role);
