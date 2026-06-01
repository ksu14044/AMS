-- Phase 12-6: 알림 ACTIVE/DISMISSED

ALTER TABLE notification
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX idx_notification_user_status ON notification (user_id, status, created_at DESC);
