ALTER TABLE comments
    ADD COLUMN parent_id BIGINT REFERENCES comments(id) ON DELETE CASCADE;

CREATE INDEX idx_comments_post_parent_created_at
    ON comments(post_id, parent_id, created_at);
