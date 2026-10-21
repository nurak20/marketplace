ALTER TABLE post_reactions
    ADD COLUMN product_id BIGINT REFERENCES products(id) ON DELETE CASCADE;

ALTER TABLE comments
    ADD COLUMN product_id BIGINT REFERENCES products(id) ON DELETE CASCADE;

ALTER TABLE post_reactions
    DROP CONSTRAINT IF EXISTS post_reactions_post_id_user_id_key;

ALTER TABLE post_reactions
    ADD CONSTRAINT uk_post_reaction_product_user UNIQUE (post_id, product_id, user_id);

CREATE UNIQUE INDEX uk_post_reaction_legacy_user
    ON post_reactions(post_id, user_id)
    WHERE product_id IS NULL;

CREATE INDEX idx_post_reactions_post_product
    ON post_reactions(post_id, product_id);

CREATE INDEX idx_comments_post_product_parent_created_at
    ON comments(post_id, product_id, parent_id, created_at);
