-- ================================================================
-- V13 : Keep uploaded images in the database
-- ================================================================
-- Container filesystems are wiped on every deploy, so product photos
-- and avatars live in PostgreSQL and are served by UploadController.

CREATE TABLE IF NOT EXISTS stored_images (
    id           UUID PRIMARY KEY,
    content_type VARCHAR(100) NOT NULL,
    data         BYTEA        NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT now()
);
