-- ================================================================
-- V12 : Realtime notifications with post + actor context
-- ================================================================

ALTER TYPE notif_type ADD VALUE IF NOT EXISTS 'POST';
ALTER TYPE notif_type ADD VALUE IF NOT EXISTS 'PASSWORD_CHANGE';

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS actor_id   BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS post_id    BIGINT REFERENCES listing_posts(id),
    ADD COLUMN IF NOT EXISTS product_id BIGINT REFERENCES products(id),
    ADD COLUMN IF NOT EXISTS item_count INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON notifications (user_id, is_read);
