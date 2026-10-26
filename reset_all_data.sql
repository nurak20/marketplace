-- Reset marketplace data while keeping the schema, Flyway history,
-- and migration-managed roles/permissions intact.
-- Run against the target database in pgAdmin or with psql.

BEGIN;

TRUNCATE TABLE
    messages,
    notifications,
    reviews,
    friendships,
    wishlists,
    comments,
    post_reactions,
    listing_post_items,
    listing_posts,
    product_images,
    products,
    user_roles,
    users
RESTART IDENTITY CASCADE;

COMMIT;

-- roles, permissions, role_permissions, and flyway_schema_history are preserved.
