-- Marketplace demo data for PostgreSQL / pgAdmin.
-- First start the Spring Boot app once so Flyway creates the schema and enums.
-- This seed is safe to run again: demo rows are matched by their stable values.
-- All 13 demo accounts use password: 12345678

BEGIN;

-- Ensure the standard access-control records exist (same names as Flyway V2).
INSERT INTO roles (name, description) VALUES
    ('ROLE_USER',  'Standard marketplace user'),
    ('ROLE_ADMIN', 'Platform administrator')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (name, description) VALUES
    ('user:read',         'View user profiles'),
    ('user:write',        'Edit own profile'),
    ('user:manage',       'Admin – manage all users'),
    ('product:read',      'View products'),
    ('product:write',     'Create / update own products'),
    ('product:manage',    'Admin – manage all products'),
    ('post:read',         'View posts'),
    ('post:write',        'Create / react to posts'),
    ('comment:write',     'Add / delete own comments'),
    ('wishlist:write',    'Add / remove wishlist items'),
    ('friend:write',      'Send / accept / decline requests'),
    ('message:write',     'Send messages'),
    ('review:write',      'Leave a review'),
    ('notification:read', 'View own notifications'),
    ('admin:access',      'Full admin panel access')
ON CONFLICT (name) DO NOTHING;

-- The V3 migration creates this placeholder admin. Reuse it for Oeurn Nurak.
UPDATE users
SET username   = 'oeurn_nurak',
    email      = 'nurak@gmail.com',
    password   = crypt('12345678', gen_salt('bf', 10)),
    full_name  = 'Oeurn Nurak',
    bio        = 'Super admin account for the marketplace demo.',
    location   = 'Phnom Penh, Cambodia',
    is_active  = TRUE,
    updated_at = NOW()
WHERE username = 'admin'
  AND email = 'admin@example.com'
  AND NOT EXISTS (SELECT 1 FROM users WHERE email = 'nurak@gmail.com');

-- The last eight users are fictional demo accounts to bring users to 13 rows.
INSERT INTO users
    (username, email, password, full_name, avatar_url, bio, location, phone, is_active)
VALUES
    ('oeurn_nurak',   'nurak@gmail.com',              crypt('12345678', gen_salt('bf', 10)), 'Oeurn Nurak',   NULL, 'Super admin account for the marketplace demo.', 'Phnom Penh, Cambodia', '+855 12 000 001', TRUE),
    ('mun_chanra',    'mun.chanra@example.com',        crypt('12345678', gen_salt('bf', 10)), 'Mun Chanra',    NULL, 'I enjoy finding useful items at a fair price.',   'Phnom Penh, Cambodia', '+855 12 000 002', TRUE),
    ('heng_sokleap',  'heng.sokleap@example.com',      crypt('12345678', gen_salt('bf', 10)), 'Heng Sokleap',  NULL, 'Selling and buying pre-loved items.',             'Kandal, Cambodia',     '+855 12 000 003', TRUE),
    ('hoem_neardey',  'hoem.neardey@example.com',      crypt('12345678', gen_salt('bf', 10)), 'Hoem Neardey',  NULL, 'Books, home goods, and little discoveries.',       'Phnom Penh, Cambodia', '+855 12 000 004', TRUE),
    ('pek_vathana',   'pek.vathana@example.com',       crypt('12345678', gen_salt('bf', 10)), 'Pek Vathana',   'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&h=400&q=80', 'Marketplace member. Profile image is a stock portrait placeholder.', 'Siem Reap, Cambodia', '+855 12 000 005', TRUE),
    ('sample_user_06','sample.user06@example.com',     crypt('12345678', gen_salt('bf', 10)), 'Sample User 06',NULL, 'Demo marketplace member.',                          'Phnom Penh, Cambodia', '+855 12 000 006', TRUE),
    ('sample_user_07','sample.user07@example.com',     crypt('12345678', gen_salt('bf', 10)), 'Sample User 07',NULL, 'Demo marketplace member.',                          'Battambang, Cambodia', '+855 12 000 007', TRUE),
    ('sample_user_08','sample.user08@example.com',     crypt('12345678', gen_salt('bf', 10)), 'Sample User 08',NULL, 'Demo marketplace member.',                          'Phnom Penh, Cambodia', '+855 12 000 008', TRUE),
    ('sample_user_09','sample.user09@example.com',     crypt('12345678', gen_salt('bf', 10)), 'Sample User 09',NULL, 'Demo marketplace member.',                          'Siem Reap, Cambodia',  '+855 12 000 009', TRUE),
    ('sample_user_10','sample.user10@example.com',     crypt('12345678', gen_salt('bf', 10)), 'Sample User 10',NULL, 'Demo marketplace member.',                          'Kampot, Cambodia',     '+855 12 000 010', TRUE),
    ('sample_user_11','sample.user11@example.com',     crypt('12345678', gen_salt('bf', 10)), 'Sample User 11',NULL, 'Demo marketplace member.',                          'Phnom Penh, Cambodia', '+855 12 000 011', TRUE),
    ('sample_user_12','sample.user12@example.com',     crypt('12345678', gen_salt('bf', 10)), 'Sample User 12',NULL, 'Demo marketplace member.',                          'Takeo, Cambodia',      '+855 12 000 012', TRUE),
    ('sample_user_13','sample.user13@example.com',     crypt('12345678', gen_salt('bf', 10)), 'Sample User 13',NULL, 'Demo marketplace member.',                          'Phnom Penh, Cambodia', '+855 12 000 013', TRUE)
ON CONFLICT (email) DO UPDATE SET
    username   = EXCLUDED.username,
    password   = EXCLUDED.password,
    full_name  = EXCLUDED.full_name,
    avatar_url = EXCLUDED.avatar_url,
    bio        = EXCLUDED.bio,
    location   = EXCLUDED.location,
    phone      = EXCLUDED.phone,
    is_active  = TRUE,
    updated_at = NOW();

-- Grant the app's standard permissions.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'user:read', 'user:write', 'product:read', 'product:write',
    'post:read', 'post:write', 'comment:write', 'wishlist:write',
    'friend:write', 'message:write', 'review:write', 'notification:read'
)
WHERE r.name = 'ROLE_USER'
ON CONFLICT DO NOTHING;

-- Stable lookups let the rest of this script work regardless of generated IDs.
CREATE TEMP TABLE seed_demo_users ON COMMIT DROP AS
SELECT v.n, u.id AS user_id, u.username
FROM (VALUES
    (1, 'oeurn_nurak'), (2, 'mun_chanra'), (3, 'heng_sokleap'),
    (4, 'hoem_neardey'), (5, 'pek_vathana'), (6, 'sample_user_06'),
    (7, 'sample_user_07'), (8, 'sample_user_08'), (9, 'sample_user_09'),
    (10, 'sample_user_10'), (11, 'sample_user_11'), (12, 'sample_user_12'),
    (13, 'sample_user_13')
) AS v(n, username)
JOIN users u ON u.username = v.username;

INSERT INTO user_roles (user_id, role_id)
SELECT du.user_id, r.id
FROM seed_demo_users du
JOIN roles r ON r.name = CASE WHEN du.n = 1 THEN 'ROLE_ADMIN' ELSE 'ROLE_USER' END
ON CONFLICT DO NOTHING;

-- Two marketplace products per demo seller. Each seller's products appear
-- together on one post so the product-scoped post logic has representative data.
WITH seed AS (
    SELECT n,
           CASE WHEN n > 13 THEN n - 13 ELSE n END AS seller_n,
           format('Marketplace demo item %s', lpad(n::text, 2, '0')) AS title,
           CASE (n - 1) % 8
               WHEN 0 THEN 'Electronics' WHEN 1 THEN 'Books'
               WHEN 2 THEN 'Home & Kitchen' WHEN 3 THEN 'Fashion'
               WHEN 4 THEN 'Sports' WHEN 5 THEN 'Accessories'
               WHEN 6 THEN 'Furniture' ELSE 'Toys'
           END AS category,
           (15 + n * 7.5)::numeric(12,2) AS price,
           CASE (n - 1) % 5
               WHEN 0 THEN 'LIKE_NEW' WHEN 1 THEN 'GOOD'
               WHEN 2 THEN 'FAIR' WHEN 3 THEN 'NEW' ELSE 'LIKE_NEW'
           END::condition_type AS item_condition
    FROM generate_series(1, 26) AS g(n)
)
INSERT INTO products (seller_id, title, description, price, category, condition, location, status)
SELECT du.user_id,
       s.title,
       'Demo listing: a well-kept ' || lower(s.category) || ' item, ready for a new owner.',
       s.price,
       s.category,
       s.item_condition,
       CASE WHEN s.n % 3 = 0 THEN 'Siem Reap, Cambodia' ELSE 'Phnom Penh, Cambodia' END,
       'AVAILABLE'::product_status
FROM seed s
JOIN seed_demo_users du ON du.n = s.seller_n
WHERE NOT EXISTS (
    SELECT 1 FROM products p WHERE p.title = s.title AND p.seller_id = du.user_id
);

CREATE TEMP TABLE seed_demo_products ON COMMIT DROP AS
SELECT right(p.title, 2)::integer AS n, p.id AS product_id, p.title, p.seller_id
FROM products p
WHERE p.title LIKE 'Marketplace demo item __'
  AND right(p.title, 2)::integer BETWEEN 1 AND 26;

-- One image per demo product. These are neutral placeholders for local testing.
INSERT INTO product_images (product_id, image_url, is_primary)
SELECT p.product_id,
       'https://placehold.co/800x600/png?text=Marketplace+item+' || lpad(p.n::text, 2, '0'),
       TRUE
FROM seed_demo_products p
WHERE NOT EXISTS (
    SELECT 1 FROM product_images i
    WHERE i.product_id = p.product_id
      AND i.image_url = 'https://placehold.co/800x600/png?text=Marketplace+item+' || lpad(p.n::text, 2, '0')
);

-- Thirteen listing posts, one per seller. The second product is linked below.
INSERT INTO listing_posts (user_id, content)
SELECT du.user_id,
       'Demo listing post ' || lpad(p.n::text, 2, '0') || ': Check out my ' || p.title || ' in Phnom Penh.'
FROM seed_demo_products p
JOIN seed_demo_users du ON du.user_id = p.seller_id
WHERE p.n BETWEEN 1 AND 13
  AND NOT EXISTS (
    SELECT 1 FROM listing_posts lp
    WHERE lp.user_id = du.user_id
      AND lp.content = 'Demo listing post ' || lpad(p.n::text, 2, '0') || ': Check out my ' || p.title || ' in Phnom Penh.'
);

CREATE TEMP TABLE seed_demo_posts ON COMMIT DROP AS
SELECT p.n, lp.id AS post_id, p.seller_id
FROM seed_demo_products p
JOIN listing_posts lp
  ON lp.user_id = p.seller_id
 AND lp.content = 'Demo listing post ' || lpad(p.n::text, 2, '0') || ': Check out my ' || p.title || ' in Phnom Penh.'
WHERE p.n BETWEEN 1 AND 13;

CREATE TEMP TABLE seed_demo_post_products ON COMMIT DROP AS
SELECT sp.n AS post_n, p.n, sp.post_id, p.product_id, p.seller_id
FROM seed_demo_posts sp
JOIN seed_demo_products p ON p.seller_id = sp.seller_id;

INSERT INTO listing_post_items (post_id, product_id)
SELECT post_id, product_id FROM seed_demo_post_products
ON CONFLICT (post_id, product_id) DO NOTHING;

-- Product-scoped reactions and comments, one set for every post/product pair.
INSERT INTO post_reactions (post_id, product_id, user_id, reaction_type)
SELECT p.post_id, p.product_id, du.user_id,
       CASE (p.n - 1) % 6
           WHEN 0 THEN 'LIKE' WHEN 1 THEN 'LOVE' WHEN 2 THEN 'HAHA'
           WHEN 3 THEN 'WOW' WHEN 4 THEN 'SAD' ELSE 'ANGRY'
       END::reaction_type
FROM seed_demo_post_products p
JOIN seed_demo_users du ON du.n = (p.n % 13) + 1
ON CONFLICT DO NOTHING;

INSERT INTO comments (post_id, product_id, user_id, content)
SELECT p.post_id, p.product_id, du.user_id,
       CASE p.n % 4
           WHEN 0 THEN 'Is this item still available?'
           WHEN 1 THEN 'Looks good. Can you share more details?'
           WHEN 2 THEN 'Would you consider meeting in Phnom Penh?'
           ELSE 'Thanks for sharing this listing.'
       END || ' Demo comment ' || lpad(p.n::text, 2, '0') || '.'
FROM seed_demo_post_products p
JOIN seed_demo_users du ON du.n = ((p.n + 1) % 13) + 1
WHERE NOT EXISTS (
    SELECT 1 FROM comments c
    WHERE c.post_id = p.post_id AND c.product_id = p.product_id AND c.user_id = du.user_id
      AND c.content = CASE p.n % 4
          WHEN 0 THEN 'Is this item still available?'
          WHEN 1 THEN 'Looks good. Can you share more details?'
          WHEN 2 THEN 'Would you consider meeting in Phnom Penh?'
          ELSE 'Thanks for sharing this listing.'
      END || ' Demo comment ' || lpad(p.n::text, 2, '0') || '.'
);

-- One wishlist entry per demo product.
INSERT INTO wishlists (user_id, product_id)
SELECT du.user_id, p.product_id
FROM seed_demo_post_products p
JOIN seed_demo_users du ON du.n = ((p.n + 2) % 13) + 1
ON CONFLICT (user_id, product_id) DO NOTHING;

-- Thirteen friend links and messages. The user pairs form a ring, with no self-links.
INSERT INTO friendships (requester_id, addressee_id, status)
SELECT a.user_id, b.user_id,
       CASE WHEN a.n % 3 = 0 THEN 'PENDING'::friend_status ELSE 'ACCEPTED'::friend_status END
FROM seed_demo_users a
JOIN seed_demo_users b ON b.n = (a.n % 13) + 1
ON CONFLICT (requester_id, addressee_id) DO NOTHING;

INSERT INTO messages (sender_id, receiver_id, product_id, content, is_read)
SELECT a.user_id, b.user_id, p.product_id,
       'Hello! I am interested in ' || p.title || '. Is it still available?',
       (a.n % 2 = 0)
FROM seed_demo_users a
JOIN seed_demo_users b ON b.n = (a.n % 13) + 1
JOIN seed_demo_products p ON p.seller_id = a.user_id AND p.n BETWEEN 1 AND 13
WHERE NOT EXISTS (
    SELECT 1 FROM messages m
    WHERE m.sender_id = a.user_id AND m.receiver_id = b.user_id
      AND m.product_id = p.product_id
      AND m.content = 'Hello! I am interested in ' || p.title || '. Is it still available?'
);

-- Thirteen notifications (ref_id is left NULL because these are display-only samples).
INSERT INTO notifications (user_id, type, message, is_read)
SELECT du.user_id,
       CASE (du.n - 1) % 5
           WHEN 0 THEN 'COMMENT' WHEN 1 THEN 'MESSAGE' WHEN 2 THEN 'FRIEND_REQUEST'
           WHEN 3 THEN 'REVIEW' ELSE 'LIKE'
       END::notif_type,
       'This is demo notification ' || lpad(du.n::text, 2, '0') || '.',
       (du.n % 2 = 0)
FROM seed_demo_users du
WHERE NOT EXISTS (
    SELECT 1 FROM notifications n
    WHERE n.user_id = du.user_id
      AND n.message = 'This is demo notification ' || lpad(du.n::text, 2, '0') || '.'
);

-- Thirteen reviews, with each buyer different from the product seller.
INSERT INTO reviews (product_id, buyer_id, seller_id, rating, comment)
SELECT p.product_id, buyer.user_id, p.seller_id,
       (3 + (p.n % 3))::smallint,
       CASE p.n % 3
           WHEN 0 THEN 'Item matched the description and pickup was easy.'
           WHEN 1 THEN 'Friendly seller and a smooth transaction.'
           ELSE 'Good value. Communication was clear.'
       END
FROM seed_demo_products p
JOIN seed_demo_users buyer ON buyer.n = (p.n % 13) + 1
WHERE p.n BETWEEN 1 AND 13
  AND NOT EXISTS (
    SELECT 1 FROM reviews r WHERE r.product_id = p.product_id AND r.buyer_id = buyer.user_id
);

COMMIT;

-- Expected demo counts: 13 users, listing_posts, friendships, messages,
-- notifications, reviews, and user_roles; 26 products, product_images,
-- listing_post_items, product-scoped post_reactions, comments, and wishlists.
-- The roles, permissions,
-- and role_permissions tables keep the app's migration-provided access data.
