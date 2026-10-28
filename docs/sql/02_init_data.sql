-- ============================================================================
-- 02 · init_data
-- ----------------------------------------------------------------------------
-- Demo dataset for UI walkthroughs. Covers every application table:
--   roles, permissions, role_permissions, users, user_roles,
--   marketplace_categories, marketplace_conditions,
--   products, product_images, listing_posts, listing_post_items,
--   comments (with replies), post_reactions, wishlists, reviews,
--   friendships, messages, notifications,
--   employee_profiles, payroll_runs, payslips.
--
-- Workflow:
--   1. Run 01_remove_table.sql (drops everything, including flyway history).
--   2. Start the Spring Boot app once so Flyway rebuilds every table (V1..V11).
--   3. Run this script in pgAdmin / psql.
--
-- Every demo account uses password: 12345678
--
-- Idempotent: users are upserted by email and every other row is matched on a
-- stable natural key, so re-running only fills in what is missing.
-- ============================================================================

BEGIN;

-- Fail fast with a helpful message when the schema has not been built yet.
DO $$
BEGIN
    IF to_regclass('public.users') IS NULL OR to_regclass('public.payslips') IS NULL THEN
        RAISE EXCEPTION 'Schema not found. Start the Spring Boot app once so Flyway creates all tables (V1..V11), then run this script.';
    END IF;
END $$;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ----------------------------------------------------------------------------
-- 1) Roles, permissions, and grants
-- ----------------------------------------------------------------------------
INSERT INTO roles (name, description) VALUES
    ('ROLE_USER',        'Standard marketplace user'),
    ('ROLE_ADMIN',       'Platform administrator'),
    ('ROLE_HR',          'Human resources staff'),
    ('ROLE_EMPLOYEE',    'Staff member with read-only management access'),
    ('ROLE_MANAGER',     'Marketplace operations manager'),
    ('ROLE_SUPER_ADMIN', 'Full platform administrator')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (name, description) VALUES
    ('user:read',         'View user profiles'),
    ('user:write',        'Edit own profile'),
    ('user:manage',       'Admin - manage all users'),
    ('product:read',      'View products'),
    ('product:write',     'Create / update own products'),
    ('product:manage',    'Admin - manage all products'),
    ('post:read',         'View posts'),
    ('post:write',        'Create / react to posts'),
    ('comment:write',     'Add / delete own comments'),
    ('wishlist:write',    'Add / remove wishlist items'),
    ('friend:write',      'Send / accept / decline requests'),
    ('message:write',     'Send messages'),
    ('review:write',      'Leave a review'),
    ('notification:read', 'View own notifications'),
    ('admin:access',      'Full admin panel access'),
    ('management:access', 'Open the staff management workspace'),
    ('category:manage',   'Create and update marketplace categories'),
    ('condition:manage',  'Enable or disable product conditions'),
    ('post:moderate',     'Remove or restore marketplace posts'),
    ('payroll:read',      'View employee compensation, payroll runs, and payslips'),
    ('payroll:manage',    'Manage employee profiles and payroll runs'),
    ('payroll:approve',   'Approve payroll runs and mark them paid'),
    ('report:read',       'View marketplace, engagement, and payroll reports')
ON CONFLICT (name) DO NOTHING;

-- ADMIN and SUPER_ADMIN receive every permission.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')
ON CONFLICT DO NOTHING;

-- Standard marketplace user.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'user:read', 'user:write',
    'product:read', 'product:write',
    'post:read', 'post:write',
    'comment:write', 'wishlist:write',
    'friend:write', 'message:write',
    'review:write', 'notification:read'
)
WHERE r.name = 'ROLE_USER'
ON CONFLICT DO NOTHING;

-- HR: management workspace, user management, payroll editing.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'management:access', 'user:manage',
    'payroll:read', 'payroll:manage',
    'report:read'
)
WHERE r.name = 'ROLE_HR'
ON CONFLICT DO NOTHING;

-- Employee: read-only management workspace.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name = 'management:access'
WHERE r.name = 'ROLE_EMPLOYEE'
ON CONFLICT DO NOTHING;

-- Manager: operations + payroll approval.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'management:access', 'user:manage', 'category:manage',
    'condition:manage', 'post:moderate',
    'payroll:read', 'payroll:approve',
    'report:read'
)
WHERE r.name = 'ROLE_MANAGER'
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------------------
-- 2) Demo accounts (password for every account: 12345678)
-- ----------------------------------------------------------------------------
INSERT INTO users
    (username, email, password, full_name, first_name, last_name,
     avatar_url, bio, location, phone, is_active, can_sell)
VALUES
    ('admin',      'admin@example.com',      crypt('12345678', gen_salt('bf', 10)),
     'System Administrator', 'System', 'Administrator',
     NULL,
     'Platform administrator for the Nurak MK27 marketplace demo.',
     'Phnom Penh, Cambodia', '+855 12 345 001', TRUE, TRUE),
    ('manager',    'manager@example.com',    crypt('12345678', gen_salt('bf', 10)),
     'Sokheng Meas', 'Sokheng', 'Meas',
     'https://picsum.photos/seed/mk27-manager/200/200',
     'Marketplace operations manager. Reviews payroll and moderates listings.',
     'Phnom Penh, Cambodia', '+855 12 345 002', TRUE, TRUE),
    ('hr',         'hr@example.com',         crypt('12345678', gen_salt('bf', 10)),
     'Dara Kim', 'Dara', 'Kim',
     'https://picsum.photos/seed/mk27-hr/200/200',
     'People operations. Runs monthly payroll and manages employee records.',
     'Phnom Penh, Cambodia', '+855 12 345 003', TRUE, TRUE),
    ('employee',   'employee@example.com',   crypt('12345678', gen_salt('bf', 10)),
     'Nita Sok', 'Nita', 'Sok',
     'https://picsum.photos/seed/mk27-employee/200/200',
     'Sales associate. Buys and sells pre-loved items on the side.',
     'Phnom Penh, Cambodia', '+855 12 345 004', TRUE, TRUE),
    ('sokha',      'sokha@example.com',      crypt('12345678', gen_salt('bf', 10)),
     'Sokha Chheng', 'Sokha', 'Chheng',
     'https://picsum.photos/seed/mk27-sokha/200/200',
     'Electronics seller. Apple gear and accessories.',
     'Phnom Penh, Cambodia', '+855 12 345 005', TRUE, TRUE),
    ('vibol',      'vibol@example.com',      crypt('12345678', gen_salt('bf', 10)),
     'Vibol Ly', 'Vibol', 'Ly',
     'https://picsum.photos/seed/mk27-vibol/200/200',
     'Outdoorsy buyer and seller. Bikes, camping gear, and scooters.',
     'Siem Reap, Cambodia', '+855 12 345 006', TRUE, TRUE),
    ('chenda',     'chenda@example.com',     crypt('12345678', gen_salt('bf', 10)),
     'Chenda Pich', 'Chenda', 'Pich',
     'https://picsum.photos/seed/mk27-chenda/200/200',
     'Home office upgrades and furniture finds.',
     'Phnom Penh, Cambodia', '+855 12 345 007', TRUE, TRUE),
    ('rithy',      'rithy@example.com',      crypt('12345678', gen_salt('bf', 10)),
     'Rithy Sam', 'Rithy', 'Sam',
     'https://picsum.photos/seed/mk27-rithy/200/200',
     'Books, board games, and music gear.',
     'Battambang, Cambodia', '+855 12 345 008', TRUE, TRUE),
    ('bopha',      'bopha@example.com',      crypt('12345678', gen_salt('bf', 10)),
     'Bopha Nou', 'Bopha', 'Nou',
     'https://picsum.photos/seed/mk27-bopha/200/200',
     'Kitchen and home appliance deals.',
     'Kandal, Cambodia', '+855 12 345 009', TRUE, TRUE),
    ('sreyneang',  'sreyneang@example.com',  crypt('12345678', gen_salt('bf', 10)),
     'Sreyneang Mao', 'Sreyneang', 'Mao',
     'https://picsum.photos/seed/mk27-sreyneang/200/200',
     'Photographer. Camera gear and fashion finds.',
     'Siem Reap, Cambodia', '+855 12 345 010', TRUE, TRUE)
ON CONFLICT (email) DO UPDATE SET
    username   = EXCLUDED.username,
    password   = EXCLUDED.password,
    full_name  = EXCLUDED.full_name,
    first_name = EXCLUDED.first_name,
    last_name  = EXCLUDED.last_name,
    avatar_url = EXCLUDED.avatar_url,
    bio        = EXCLUDED.bio,
    location   = EXCLUDED.location,
    phone      = EXCLUDED.phone,
    is_active  = TRUE,
    can_sell   = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- Everyone gets ROLE_USER; staff accounts add their workspace role.
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_USER'
WHERE u.username IN ('admin', 'manager', 'hr', 'employee', 'sokha', 'vibol',
                     'chenda', 'rithy', 'bopha', 'sreyneang')
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM (VALUES
    ('admin',    'ROLE_ADMIN'),
    ('manager',  'ROLE_MANAGER'),
    ('hr',       'ROLE_HR'),
    ('employee', 'ROLE_EMPLOYEE')
) AS v(username, role_name)
JOIN users u ON u.username = v.username
JOIN roles r ON r.name = v.role_name
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------------------
-- 3) Marketplace catalog
-- ----------------------------------------------------------------------------
INSERT INTO marketplace_categories (name) VALUES
    ('Electronics'), ('Fashion'), ('Home & Furniture'), ('Vehicles'),
    ('Books & Hobbies'), ('Sports & Outdoors'), ('Toys & Games'), ('Collectibles')
ON CONFLICT (name) DO NOTHING;

UPDATE marketplace_categories
SET active = TRUE, updated_at = NOW()
WHERE name IN ('Electronics', 'Fashion', 'Home & Furniture', 'Vehicles',
               'Books & Hobbies', 'Sports & Outdoors', 'Toys & Games');

-- One inactive category demonstrates the management toggle.
UPDATE marketplace_categories
SET active = FALSE, updated_at = NOW()
WHERE name = 'Collectibles';

INSERT INTO marketplace_conditions (code) VALUES
    ('NEW'), ('LIKE_NEW'), ('GOOD'), ('FAIR'), ('POOR')
ON CONFLICT (code) DO NOTHING;

UPDATE marketplace_conditions SET active = TRUE;

-- ----------------------------------------------------------------------------
-- 4) Employee profiles (payroll)
-- ----------------------------------------------------------------------------
INSERT INTO employee_profiles
    (user_id, employee_code, job_title, department, base_salary, allowance,
     currency, hire_date, bank_name, bank_account, status)
SELECT u.id, v.employee_code, v.job_title, v.department, v.base_salary,
       v.allowance, 'USD', v.hire_date, v.bank_name, v.bank_account,
       v.status::employment_status
FROM (VALUES
    ('employee',  'EMP-0001', 'Sales Associate',      'Sales',       650.00, 50.00, DATE '2023-03-01', 'ABA Bank',    '000-123-001', 'ACTIVE'),
    ('sokha',     'EMP-0002', 'Warehouse Supervisor', 'Operations',  720.00, 60.00, DATE '2021-08-16', 'ACLEDA Bank', '000-123-002', 'ACTIVE'),
    ('vibol',     'EMP-0003', 'Delivery Driver',      'Logistics',   480.00, 40.00, DATE '2022-05-09', 'ABA Bank',    '000-123-003', 'ACTIVE'),
    ('chenda',    'EMP-0004', 'Customer Support',     'Support',     520.00, 35.00, DATE '2023-11-20', 'Wing Bank',   '000-123-004', 'ON_LEAVE'),
    ('rithy',     'EMP-0005', 'Marketing Specialist', 'Marketing',   680.00, 45.00, DATE '2022-02-14', 'ACLEDA Bank', '000-123-005', 'ACTIVE'),
    ('bopha',     'EMP-0006', 'Accountant',           'Finance',     850.00, 70.00, DATE '2020-09-01', 'ABA Bank',    '000-123-006', 'ACTIVE'),
    ('sreyneang', 'EMP-0007', 'Product Photographer', 'Marketing',   560.00, 30.00, DATE '2024-01-08', 'Wing Bank',   '000-123-007', 'ACTIVE'),
    ('hr',        'EMP-0008', 'HR Officer',           'People',      780.00, 60.00, DATE '2021-06-21', 'ACLEDA Bank', '000-123-008', 'ACTIVE')
) AS v(username, employee_code, job_title, department, base_salary, allowance,
       hire_date, bank_name, bank_account, status)
JOIN users u ON u.username = v.username
ON CONFLICT (user_id) DO UPDATE SET
    employee_code = EXCLUDED.employee_code,
    job_title     = EXCLUDED.job_title,
    department    = EXCLUDED.department,
    base_salary   = EXCLUDED.base_salary,
    allowance     = EXCLUDED.allowance,
    currency      = EXCLUDED.currency,
    hire_date     = EXCLUDED.hire_date,
    bank_name     = EXCLUDED.bank_name,
    bank_account  = EXCLUDED.bank_account,
    status        = EXCLUDED.status,
    updated_at    = NOW();

-- ----------------------------------------------------------------------------
-- 5) Products
-- ----------------------------------------------------------------------------
CREATE TEMP TABLE seed_product_rows (
    slug           TEXT PRIMARY KEY,
    username       TEXT NOT NULL,
    title          TEXT NOT NULL,
    description    TEXT NOT NULL,
    price          NUMERIC(12,2) NOT NULL,
    category       TEXT NOT NULL,
    item_condition TEXT NOT NULL,
    location       TEXT NOT NULL,
    item_status    TEXT NOT NULL,
    age_days       INTEGER NOT NULL
) ON COMMIT DROP;

INSERT INTO seed_product_rows VALUES
    ('ip13',      'sokha',     'iPhone 13 Pro 256GB',
     'Unlocked 256GB in Graphite. Battery health 92%, includes the original box, cable, and a clear case.',
     620.00, 'Electronics', 'LIKE_NEW', 'Phnom Penh, Cambodia', 'AVAILABLE', 9),
    ('airpods',   'sokha',     'Apple AirPods Pro (2nd gen)',
     'Second-generation AirPods Pro with MagSafe charging case. Ear tips refreshed, no scratches.',
     145.00, 'Electronics', 'GOOD', 'Phnom Penh, Cambodia', 'AVAILABLE', 12),
    ('macbook',   'sokha',     'MacBook Air M1 13-inch',
     'M1 MacBook Air, 8GB RAM, 256GB SSD. Light use and already sold to a colleague.',
     540.00, 'Electronics', 'LIKE_NEW', 'Phnom Penh, Cambodia', 'SOLD', 30),
    ('bike',      'vibol',     'Trek Marlin 6 Mountain Bike',
     'Size M Trek Marlin 6 with hydraulic disc brakes. Recently serviced and includes a bottle cage.',
     380.00, 'Sports & Outdoors', 'GOOD', 'Siem Reap, Cambodia', 'AVAILABLE', 8),
    ('tent',      'vibol',     '4-Person Camping Tent',
     'Four-person dome tent, used twice. Waterproof rainfly and carry bag included.',
     75.00, 'Sports & Outdoors', 'LIKE_NEW', 'Siem Reap, Cambodia', 'AVAILABLE', 15),
    ('scooter',   'vibol',     'Honda Click 125i 2021',
     '2021 Honda Click 125i with 18,000 km and a full service history. Reserved for a buyer this week.',
     1250.00, 'Vehicles', 'GOOD', 'Siem Reap, Cambodia', 'RESERVED', 2),
    ('desk',      'chenda',    'Solid Wood Study Desk',
     'Solid acacia wood desk, 120 x 60 cm. Minor scratch on the left leg, otherwise great.',
     95.00, 'Home & Furniture', 'GOOD', 'Phnom Penh, Cambodia', 'AVAILABLE', 11),
    ('chair',     'chenda',    'Ergonomic Office Chair',
     'Ergonomic mesh chair with lumbar support and adjustable armrests. No tears.',
     120.00, 'Home & Furniture', 'LIKE_NEW', 'Phnom Penh, Cambodia', 'AVAILABLE', 11),
    ('lamp',      'chenda',    'Nordic Floor Lamp',
     'Nordic-style floor lamp with a warm LED bulb included. Brand new in an unopened box.',
     35.00, 'Home & Furniture', 'NEW', 'Phnom Penh, Cambodia', 'SOLD', 25),
    ('novels',    'rithy',     'Box of 12 Classic Novels',
     'Twelve classic paperbacks including Orwell, Austen, and Hemingway. Some yellowed pages.',
     28.00, 'Books & Hobbies', 'FAIR', 'Battambang, Cambodia', 'AVAILABLE', 10),
    ('boardgame', 'rithy',     'Catan Board Game',
     'Catan base game, complete with all cards and pieces. Played a handful of times.',
     22.00, 'Books & Hobbies', 'LIKE_NEW', 'Battambang, Cambodia', 'AVAILABLE', 10),
    ('guitar',    'rithy',     'Yamaha F310 Acoustic Guitar',
     'Yamaha F310 acoustic guitar with soft case, strap, and capo. Fresh strings.',
     85.00, 'Books & Hobbies', 'GOOD', 'Battambang, Cambodia', 'AVAILABLE', 18),
    ('blender',   'bopha',     'Philips High-Speed Blender',
     'Philips 600W blender with two jars. Works perfectly, upgraded to a larger model.',
     45.00, 'Home & Furniture', 'GOOD', 'Kandal, Cambodia', 'AVAILABLE', 7),
    ('airfryer',  'bopha',     '5L Digital Air Fryer',
     '5L digital air fryer with 8 presets. Basket cleaned, no odors or scratches.',
     60.00, 'Home & Furniture', 'LIKE_NEW', 'Kandal, Cambodia', 'AVAILABLE', 7),
    ('camera',    'sreyneang', 'Canon EOS M50 Mark II',
     'Canon EOS M50 Mark II with 15-45mm kit lens. Shutter count under 8,000.',
     430.00, 'Electronics', 'GOOD', 'Siem Reap, Cambodia', 'AVAILABLE', 6),
    ('lens',      'sreyneang', 'Canon EF-M 22mm f/2 Lens',
     'Canon EF-M 22mm f/2 pancake lens. Sharp and compact, includes both caps.',
     150.00, 'Electronics', 'LIKE_NEW', 'Siem Reap, Cambodia', 'AVAILABLE', 6),
    ('jacket',    'employee',  'Uniqlo Down Jacket (M)',
     'Uniqlo ultra light down jacket, size M, navy. Worn twice with no rips or stains.',
     32.00, 'Fashion', 'LIKE_NEW', 'Phnom Penh, Cambodia', 'AVAILABLE', 5),
    ('sneakers',  'employee',  'Nike Air Zoom Pegasus 39',
     'Nike Air Zoom Pegasus 39, US 9, black. About 50 km of running, good tread.',
     55.00, 'Fashion', 'GOOD', 'Phnom Penh, Cambodia', 'AVAILABLE', 5),
    ('watch',     'employee',  'Casio Edifice Chronograph',
     'Casio Edifice chronograph with a stainless steel band. New battery fitted last month.',
     78.00, 'Fashion', 'GOOD', 'Phnom Penh, Cambodia', 'AVAILABLE', 4),
    ('bag',       'employee',  'Leather Laptop Backpack',
     'Full-grain leather laptop backpack that fits 15-inch laptops. Light wear on the base.',
     42.00, 'Fashion', 'LIKE_NEW', 'Phnom Penh, Cambodia', 'AVAILABLE', 4);

INSERT INTO products
    (seller_id, title, description, price, category, condition, location,
     status, created_at, updated_at)
SELECT u.id, p.title, p.description, p.price, p.category, p.item_condition,
       p.location, p.item_status::product_status,
       NOW() - make_interval(days => p.age_days),
       NOW() - make_interval(days => p.age_days)
FROM seed_product_rows p
JOIN users u ON u.username = p.username
WHERE NOT EXISTS (
    SELECT 1 FROM products x WHERE x.seller_id = u.id AND x.title = p.title
);

CREATE TEMP TABLE seed_products ON COMMIT DROP AS
SELECT p.slug, x.id AS product_id, x.seller_id
FROM seed_product_rows p
JOIN users u ON u.username = p.username
JOIN products x ON x.seller_id = u.id AND x.title = p.title;

-- Primary image for every product.
INSERT INTO product_images (product_id, image_url, is_primary)
SELECT s.product_id,
       'https://picsum.photos/seed/mk27-' || s.slug || '/800/600',
       TRUE
FROM seed_products s
WHERE NOT EXISTS (
    SELECT 1 FROM product_images i
    WHERE i.product_id = s.product_id
      AND i.image_url = 'https://picsum.photos/seed/mk27-' || s.slug || '/800/600'
);

-- Extra gallery images for a few listings.
INSERT INTO product_images (product_id, image_url, is_primary)
SELECT s.product_id,
       'https://picsum.photos/seed/mk27-' || s.slug || '-b/800/600',
       FALSE
FROM seed_products s
WHERE s.slug IN ('ip13', 'bike', 'camera')
  AND NOT EXISTS (
    SELECT 1 FROM product_images i
    WHERE i.product_id = s.product_id
      AND i.image_url = 'https://picsum.photos/seed/mk27-' || s.slug || '-b/800/600'
);

-- ----------------------------------------------------------------------------
-- 6) Listing posts and their products
-- ----------------------------------------------------------------------------
CREATE TEMP TABLE seed_post_rows (
    post_key  TEXT PRIMARY KEY,
    username  TEXT NOT NULL,
    content   TEXT NOT NULL,
    age_days  INTEGER NOT NULL
) ON COMMIT DROP;

INSERT INTO seed_post_rows VALUES
    ('p-sokha',    'sokha',     'Saying goodbye to my iPhone 13 Pro and AirPods Pro. Both are well cared for, so message me if you are interested!', 6),
    ('p-vibol',    'vibol',     'Weekend clear-out: my Trek mountain bike and camping tent are looking for a new home.', 8),
    ('p-chenda',   'chenda',    'Refreshing my home office, so my study desk and ergonomic chair are up for sale.', 11),
    ('p-rithy',    'rithy',     'Books, board games, and a guitar. A perfect starter bundle for a quiet weekend.', 10),
    ('p-bopha',    'bopha',     'Kitchen upgrade! Blender and air fryer, both lightly used and ready for a new kitchen.', 7),
    ('p-sreyneang','sreyneang', 'Selling my Canon M50 kit and 22mm lens. Great starter setup for content creators.', 6),
    ('p-employee', 'employee',  'A few wardrobe extras: down jacket, Pegasus sneakers, and a Casio watch.', 5),
    ('p-scooter',  'vibol',     'Honda Click 125i is reserved for a buyer this week. Thanks for all the messages!', 2);

INSERT INTO listing_posts (user_id, content, created_at, updated_at)
SELECT u.id, p.content,
       NOW() - make_interval(days => p.age_days),
       NOW() - make_interval(days => p.age_days)
FROM seed_post_rows p
JOIN users u ON u.username = p.username
WHERE NOT EXISTS (
    SELECT 1 FROM listing_posts x WHERE x.user_id = u.id AND x.content = p.content
);

CREATE TEMP TABLE seed_posts ON COMMIT DROP AS
SELECT r.post_key, p.id AS post_id
FROM seed_post_rows r
JOIN users u ON u.username = r.username
JOIN listing_posts p ON p.user_id = u.id AND p.content = r.content;

INSERT INTO listing_post_items (post_id, product_id)
SELECT s.post_id, pi.product_id
FROM (VALUES
    ('p-sokha', 'ip13'), ('p-sokha', 'airpods'),
    ('p-vibol', 'bike'), ('p-vibol', 'tent'),
    ('p-chenda', 'desk'), ('p-chenda', 'chair'),
    ('p-rithy', 'novels'), ('p-rithy', 'boardgame'), ('p-rithy', 'guitar'),
    ('p-bopha', 'blender'), ('p-bopha', 'airfryer'),
    ('p-sreyneang', 'camera'), ('p-sreyneang', 'lens'),
    ('p-employee', 'jacket'), ('p-employee', 'sneakers'), ('p-employee', 'watch'),
    ('p-scooter', 'scooter')
) AS v(post_key, product_slug)
JOIN seed_posts s ON s.post_key = v.post_key
JOIN seed_products pi ON pi.slug = v.product_slug
ON CONFLICT (post_id, product_id) DO NOTHING;

-- One soft-removed post so the moderation view has data.
UPDATE listing_posts p
SET deleted_at = NOW() - INTERVAL '1 day', updated_at = NOW()
FROM seed_posts s
WHERE p.id = s.post_id
  AND s.post_key = 'p-scooter'
  AND p.deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 7) Comments (top-level, nested replies, and product-scoped)
-- ----------------------------------------------------------------------------
CREATE TEMP TABLE seed_comment_rows (
    comment_key  TEXT PRIMARY KEY,
    post_key     TEXT NOT NULL,
    parent_key   TEXT,
    product_slug TEXT,
    username     TEXT NOT NULL,
    content      TEXT NOT NULL,
    hours_ago    INTEGER NOT NULL
) ON COMMIT DROP;

INSERT INTO seed_comment_rows VALUES
    ('c1',  'p-sokha',     NULL, 'ip13',      'chenda',    'Is the iPhone still available? Does it come with the original box?', 70),
    ('c2',  'p-sokha',     NULL, 'airpods',   'rithy',     'How is the battery life on the AirPods?', 52),
    ('c3',  'p-vibol',     NULL, 'bike',      'employee',  'What frame size is the Trek? I am about 175 cm tall.', 48),
    ('c4',  'p-chenda',    NULL, 'desk',      'bopha',     'Is the desk solid wood or veneer?', 40),
    ('c5',  'p-rithy',     NULL, 'novels',    'sreyneang', 'Which titles are included in the box?', 36),
    ('c6',  'p-bopha',     NULL, 'airfryer',  'sokha',     'How many litres is the air fryer?', 30),
    ('c7',  'p-sreyneang', NULL, 'camera',    'employee',  'Does the M50 come with the kit lens too?', 26),
    ('c8',  'p-sokha',     NULL, NULL,        'vibol',     'Great photos. Good luck with the sale!', 20),
    ('c9',  'p-employee',  NULL, 'watch',     'chenda',    'Is the watch still under warranty?', 18),
    ('c10', 'p-chenda',    NULL, 'chair',     'rithy',     'Does the chair recline, and is the gas lift firm?', 14),

    ('c11', 'p-sokha',     'c1', 'ip13',      'sokha',     'Yes, still available. The box, cable, and a clear case are included.', 66),
    ('c12', 'p-sokha',     'c2', 'airpods',   'vibol',     'About four hours with noise cancelling on. The case charges fine.', 50),
    ('c13', 'p-vibol',     'c3', 'bike',      'vibol',     'It is a size M frame, which fits around 170 to 180 cm.', 46),
    ('c14', 'p-chenda',    'c4', 'desk',      'chenda',    'Solid acacia wood with a natural oil finish.', 38),
    ('c15', 'p-rithy',     'c5', 'novels',    'rithy',     'Orwell, Austen, Hemingway, and nine more classics. I can send a photo.', 34),
    ('c16', 'p-employee',  'c9', 'watch',     'employee',  'Yes, the warranty runs until March 2027.', 16);

INSERT INTO comments (post_id, product_id, user_id, content, created_at, updated_at)
SELECT s.post_id, pi.product_id, u.id, c.content,
       NOW() - make_interval(hours => c.hours_ago),
       NOW() - make_interval(hours => c.hours_ago)
FROM seed_comment_rows c
JOIN seed_posts s ON s.post_key = c.post_key
JOIN users u ON u.username = c.username
LEFT JOIN seed_products pi ON pi.slug = c.product_slug
WHERE c.parent_key IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM comments x
      WHERE x.post_id = s.post_id AND x.user_id = u.id AND x.content = c.content
  );

INSERT INTO comments (post_id, product_id, user_id, parent_id, content, created_at, updated_at)
SELECT s.post_id, pi.product_id, u.id, parent.id, c.content,
       NOW() - make_interval(hours => c.hours_ago),
       NOW() - make_interval(hours => c.hours_ago)
FROM seed_comment_rows c
JOIN seed_comment_rows pc ON pc.comment_key = c.parent_key
JOIN seed_posts s ON s.post_key = c.post_key
JOIN users u ON u.username = c.username
JOIN comments parent ON parent.post_id = s.post_id AND parent.content = pc.content
LEFT JOIN seed_products pi ON pi.slug = c.product_slug
WHERE c.parent_key IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM comments x
      WHERE x.post_id = s.post_id AND x.user_id = u.id AND x.content = c.content
  );

-- ----------------------------------------------------------------------------
-- 8) Post reactions
-- ----------------------------------------------------------------------------
INSERT INTO post_reactions (post_id, product_id, user_id, reaction_type, created_at)
SELECT s.post_id, pi.product_id, u.id, r.reaction::reaction_type,
       NOW() - make_interval(hours => r.hours_ago)
FROM (VALUES
    ('p-sokha', 'ip13', 'chenda', 'LIKE', 65),
    ('p-sokha', 'ip13', 'vibol', 'LOVE', 60),
    ('p-sokha', 'airpods', 'rithy', 'LIKE', 49),
    ('p-sokha', NULL, 'employee', 'LOVE', 18),
    ('p-vibol', 'bike', 'employee', 'LIKE', 45),
    ('p-vibol', 'bike', 'chenda', 'WOW', 42),
    ('p-chenda', 'desk', 'bopha', 'LIKE', 37),
    ('p-chenda', 'chair', 'rithy', 'HAHA', 12),
    ('p-rithy', 'novels', 'sreyneang', 'LOVE', 33),
    ('p-rithy', 'guitar', 'sokha', 'WOW', 28),
    ('p-bopha', 'airfryer', 'sokha', 'LIKE', 27),
    ('p-sreyneang', 'camera', 'employee', 'LOVE', 24),
    ('p-employee', 'watch', 'chenda', 'LIKE', 15),
    ('p-employee', 'bag', 'bopha', 'SAD', 10)
) AS r(post_key, product_slug, username, reaction, hours_ago)
JOIN seed_posts s ON s.post_key = r.post_key
JOIN users u ON u.username = r.username
LEFT JOIN seed_products pi ON pi.slug = r.product_slug
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------------------
-- 9) Wishlists
-- ----------------------------------------------------------------------------
INSERT INTO wishlists (user_id, product_id, created_at)
SELECT u.id, pi.product_id, NOW() - make_interval(days => w.age_days)
FROM (VALUES
    ('employee', 'ip13', 8), ('employee', 'camera', 5), ('employee', 'bike', 3),
    ('chenda', 'ip13', 6), ('chenda', 'jacket', 4),
    ('rithy', 'camera', 5), ('rithy', 'lens', 5), ('rithy', 'jacket', 2),
    ('bopha', 'desk', 9), ('bopha', 'ip13', 4),
    ('vibol', 'sneakers', 4), ('vibol', 'watch', 3),
    ('sreyneang', 'bag', 3), ('sreyneang', 'airpods', 2),
    ('sokha', 'boardgame', 9), ('sokha', 'tent', 7)
) AS w(username, product_slug, age_days)
JOIN users u ON u.username = w.username
JOIN seed_products pi ON pi.slug = w.product_slug
ON CONFLICT (user_id, product_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 10) Reviews
-- ----------------------------------------------------------------------------
INSERT INTO reviews (product_id, buyer_id, seller_id, rating, comment, created_at)
SELECT pi.product_id, buyer.id, pi.seller_id, r.rating, r.comment,
       NOW() - make_interval(days => r.age_days)
FROM (VALUES
    ('macbook', 'employee', 5, 'MacBook arrived exactly as described. Smooth handover.', 2),
    ('lamp', 'sreyneang', 4, 'Lamp looks great in my living room. Packaging was careful.', 3),
    ('guitar', 'bopha', 5, 'Guitar sounds lovely and the setup was already done.', 4),
    ('ip13', 'chenda', 5, 'Phone is in excellent condition. Friendly seller.', 1)
) AS r(product_slug, buyer_username, rating, comment, age_days)
JOIN seed_products pi ON pi.slug = r.product_slug
JOIN users buyer ON buyer.username = r.buyer_username
WHERE buyer.id <> pi.seller_id
ON CONFLICT (product_id, buyer_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 11) Friendships
-- ----------------------------------------------------------------------------
INSERT INTO friendships (requester_id, addressee_id, status, created_at, updated_at)
SELECT requester.id, addressee.id, f.status::friend_status,
       NOW() - make_interval(days => f.age_days),
       NOW() - make_interval(days => f.age_days)
FROM (VALUES
    ('sokha', 'vibol', 'ACCEPTED', 40),
    ('sokha', 'chenda', 'ACCEPTED', 35),
    ('chenda', 'rithy', 'ACCEPTED', 30),
    ('bopha', 'sreyneang', 'ACCEPTED', 25),
    ('employee', 'sokha', 'ACCEPTED', 20),
    ('hr', 'manager', 'PENDING', 5),
    ('rithy', 'bopha', 'PENDING', 4),
    ('vibol', 'employee', 'PENDING', 2)
) AS f(requester, addressee, status, age_days)
JOIN users requester ON requester.username = f.requester
JOIN users addressee ON addressee.username = f.addressee
WHERE requester.id <> addressee.id
ON CONFLICT (requester_id, addressee_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 12) Messages
-- ----------------------------------------------------------------------------
INSERT INTO messages (sender_id, receiver_id, product_id, content, is_read, created_at)
SELECT sender.id, receiver.id, pi.product_id, m.content, m.is_read,
       NOW() - make_interval(hours => m.hours_ago)
FROM (VALUES
    ('chenda', 'sokha', 'ip13', 'Hi Sokha, I am interested in the iPhone. Can we meet near BKK1 tomorrow?', FALSE, 22),
    ('sokha', 'chenda', 'ip13', 'Sure, 5 PM at Brown Coffee works for me.', TRUE, 21),
    ('employee', 'vibol', 'bike', 'Hello, is the Trek still available? What size is the frame?', FALSE, 12),
    ('bopha', 'chenda', 'desk', 'I would like to see the desk this weekend if possible.', TRUE, 30),
    ('rithy', 'sreyneang', 'camera', 'Does the camera include a spare battery?', FALSE, 8),
    ('manager', 'hr', NULL, 'Please share the October payroll draft before Friday.', TRUE, 48)
) AS m(sender, receiver, product_slug, content, is_read, hours_ago)
JOIN users sender ON sender.username = m.sender
JOIN users receiver ON receiver.username = m.receiver
LEFT JOIN seed_products pi ON pi.slug = m.product_slug
WHERE sender.id <> receiver.id
  AND NOT EXISTS (
      SELECT 1 FROM messages x
      WHERE x.sender_id = sender.id AND x.receiver_id = receiver.id
        AND x.content = m.content
  );

-- ----------------------------------------------------------------------------
-- 13) Payroll runs and payslips (August 2026 paid, September approved,
--     October draft so every workflow step is demoable)
-- ----------------------------------------------------------------------------
INSERT INTO payroll_runs
    (period_year, period_month, currency, status, notes, created_by,
     approved_by, approved_at, paid_at, created_at, updated_at)
SELECT v.period_year, v.period_month, 'USD', v.status::payroll_run_status,
       v.notes, creator.id,
       CASE WHEN v.status IN ('APPROVED', 'PAID') THEN approver.id END,
       CASE WHEN v.status IN ('APPROVED', 'PAID') THEN v.approved_at END,
       CASE WHEN v.status = 'PAID' THEN v.paid_at END,
       COALESCE(v.approved_at, NOW()) - INTERVAL '6 days',
       COALESCE(v.paid_at, v.approved_at, NOW())
FROM (VALUES
    (2026, 8, 'PAID',
     'August 2026 payroll - approved and paid.',
     TIMESTAMPTZ '2026-08-28 10:00:00+07',
     TIMESTAMPTZ '2026-08-30 09:00:00+07'),
    (2026, 9, 'APPROVED',
     'September 2026 payroll - approved, awaiting payment.',
     TIMESTAMPTZ '2026-09-28 10:00:00+07',
     NULL::timestamptz),
    (2026, 10, 'DRAFT',
     'October 2026 payroll - draft for review.',
     NULL::timestamptz,
     NULL::timestamptz)
) AS v(period_year, period_month, status, notes, approved_at, paid_at)
JOIN users creator ON creator.username = 'hr'
JOIN users approver ON approver.username = 'manager'
ON CONFLICT (period_year, period_month) DO NOTHING;

INSERT INTO payslips
    (run_id, user_id, employee_code, employee_name, job_title, department,
     base_salary, allowance, bonus, overtime, tax, other_deductions,
     gross_pay, deductions_total, net_pay, currency)
SELECT r.id, u.id, e.employee_code, COALESCE(u.full_name, u.username),
       e.job_title, e.department, e.base_salary, e.allowance,
       v.bonus, v.overtime,
       ROUND((e.base_salary + e.allowance + v.bonus + v.overtime) * v.tax_rate, 2),
       v.other,
       e.base_salary + e.allowance + v.bonus + v.overtime,
       ROUND((e.base_salary + e.allowance + v.bonus + v.overtime) * v.tax_rate, 2) + v.other,
       (e.base_salary + e.allowance + v.bonus + v.overtime)
           - (ROUND((e.base_salary + e.allowance + v.bonus + v.overtime) * v.tax_rate, 2) + v.other),
       e.currency
FROM payroll_runs r
CROSS JOIN employee_profiles e
JOIN users u ON u.id = e.user_id
JOIN (VALUES
    (8, 'employee',  20.00, 4.00,  0.05, 0.00),
    (8, 'sokha',     30.00, 6.00,  0.05, 10.00),
    (8, 'vibol',     15.00, 8.00,  0.04, 0.00),
    (8, 'chenda',    10.00, 0.00,  0.04, 0.00),
    (8, 'rithy',     25.00, 3.00,  0.05, 5.00),
    (8, 'bopha',     40.00, 0.00,  0.06, 0.00),
    (8, 'sreyneang', 20.00, 2.00,  0.04, 0.00),
    (8, 'hr',        35.00, 0.00,  0.05, 0.00),
    (9, 'employee',  25.00, 6.00,  0.05, 0.00),
    (9, 'sokha',     35.00, 4.00,  0.05, 0.00),
    (9, 'vibol',     20.00, 10.00, 0.04, 0.00),
    (9, 'chenda',    15.00, 0.00,  0.04, 5.00),
    (9, 'rithy',     30.00, 5.00,  0.05, 0.00),
    (9, 'bopha',     45.00, 0.00,  0.06, 0.00),
    (9, 'sreyneang', 25.00, 3.00,  0.04, 0.00),
    (9, 'hr',        40.00, 0.00,  0.05, 0.00),
    (10, 'employee',  0.00, 8.00,  0.05, 0.00),
    (10, 'sokha',     0.00, 6.00,  0.05, 0.00),
    (10, 'vibol',     0.00, 12.00, 0.04, 0.00),
    (10, 'chenda',    0.00, 4.00,  0.04, 0.00),
    (10, 'rithy',     0.00, 6.00,  0.05, 0.00),
    (10, 'bopha',     0.00, 2.00,  0.06, 0.00),
    (10, 'sreyneang', 0.00, 5.00,  0.04, 0.00),
    (10, 'hr',        0.00, 4.00,  0.05, 0.00)
) AS v(period_month, username, bonus, overtime, tax_rate, other)
    ON v.username = u.username AND v.period_month = r.period_month
WHERE r.period_year = 2026
  AND r.period_month IN (8, 9, 10)
ON CONFLICT (run_id, user_id) DO NOTHING;

-- Keep the run summary in sync with its payslips.
UPDATE payroll_runs r
SET total_gross = t.gross,
    total_deductions = t.deductions,
    total_net = t.net,
    updated_at = NOW()
FROM (
    SELECT run_id,
           SUM(gross_pay) AS gross,
           SUM(deductions_total) AS deductions,
           SUM(net_pay) AS net
    FROM payslips
    GROUP BY run_id
) t
WHERE t.run_id = r.id;

-- ----------------------------------------------------------------------------
-- 14) Notifications
-- ----------------------------------------------------------------------------
INSERT INTO notifications (user_id, type, message, is_read, created_at)
SELECT u.id, n.type::notif_type, n.message, n.is_read,
       NOW() - make_interval(hours => n.hours_ago)
FROM (VALUES
    ('employee', 'COMMENT', 'Chenda Pich commented on your watch listing.', FALSE, 16),
    ('sokha', 'MESSAGE', 'Chenda Pich sent you a message about iPhone 13 Pro 256GB.', TRUE, 21),
    ('sokha', 'LIKE', 'Vibol Ly reacted to your iPhone listing.', FALSE, 44),
    ('vibol', 'REVIEW', 'You received a new review for your MacBook Air.', TRUE, 30),
    ('chenda', 'FRIEND_REQUEST', 'Rithy Sam sent you a friend request.', FALSE, 12),
    ('bopha', 'COMMENT', 'Sokha Chheng commented on your air fryer listing.', TRUE, 29),
    ('hr', 'PAYROLL', 'October 2026 payroll is still in draft.', FALSE, 3),
    ('manager', 'FRIEND_REQUEST', 'Dara Kim wants to connect with you.', FALSE, 10)
) AS n(username, type, message, is_read, hours_ago)
JOIN users u ON u.username = n.username
WHERE NOT EXISTS (
    SELECT 1 FROM notifications x
    WHERE x.user_id = u.id AND x.message = n.message
);

-- One payslip notification per employee for the approved September run.
INSERT INTO notifications (user_id, type, message, ref_id, is_read, created_at)
SELECT p.user_id, 'PAYROLL', 'Your September 2026 payslip is ready.', p.id,
       FALSE, r.approved_at + INTERVAL '1 hour'
FROM payslips p
JOIN payroll_runs r ON r.id = p.run_id
WHERE r.period_year = 2026
  AND r.period_month = 9
  AND NOT EXISTS (
      SELECT 1 FROM notifications x
      WHERE x.type = 'PAYROLL' AND x.ref_id = p.id
  );

COMMIT;

-- ----------------------------------------------------------------------------
-- Verification
-- ----------------------------------------------------------------------------
-- Row counts for every seeded table.
SELECT 'roles' AS table_name, count(*) AS rows FROM roles
UNION ALL SELECT 'permissions', count(*) FROM permissions
UNION ALL SELECT 'role_permissions', count(*) FROM role_permissions
UNION ALL SELECT 'users', count(*) FROM users
UNION ALL SELECT 'user_roles', count(*) FROM user_roles
UNION ALL SELECT 'marketplace_categories', count(*) FROM marketplace_categories
UNION ALL SELECT 'marketplace_conditions', count(*) FROM marketplace_conditions
UNION ALL SELECT 'employee_profiles', count(*) FROM employee_profiles
UNION ALL SELECT 'products', count(*) FROM products
UNION ALL SELECT 'product_images', count(*) FROM product_images
UNION ALL SELECT 'listing_posts', count(*) FROM listing_posts
UNION ALL SELECT 'listing_post_items', count(*) FROM listing_post_items
UNION ALL SELECT 'comments', count(*) FROM comments
UNION ALL SELECT 'post_reactions', count(*) FROM post_reactions
UNION ALL SELECT 'wishlists', count(*) FROM wishlists
UNION ALL SELECT 'reviews', count(*) FROM reviews
UNION ALL SELECT 'friendships', count(*) FROM friendships
UNION ALL SELECT 'messages', count(*) FROM messages
UNION ALL SELECT 'notifications', count(*) FROM notifications
UNION ALL SELECT 'payroll_runs', count(*) FROM payroll_runs
UNION ALL SELECT 'payslips', count(*) FROM payslips
ORDER BY table_name;

-- Demo logins. Every password is 12345678.
SELECT u.username, u.email, u.full_name,
       COALESCE(string_agg(r.name, ', ' ORDER BY r.name), '(no roles)') AS roles,
       (u.password = crypt('12345678', u.password)) AS password_is_12345678
FROM users u
LEFT JOIN user_roles ur ON ur.user_id = u.id
LEFT JOIN roles r ON r.id = ur.role_id
WHERE u.username IN ('admin', 'manager', 'hr', 'employee', 'sokha', 'vibol',
                     'chenda', 'rithy', 'bopha', 'sreyneang')
GROUP BY u.id, u.username, u.email, u.full_name, u.password
ORDER BY u.username;

-- Payroll integrity: expect 0 rows for each column.
SELECT count(*) FILTER (
           WHERE gross_pay <> base_salary + allowance + bonus + overtime
              OR deductions_total <> tax + other_deductions
              OR net_pay <> gross_pay - deductions_total
       ) AS invalid_payslips,
       count(*) FILTER (WHERE net_pay < 0) AS negative_net_payslips
FROM payslips;

SELECT r.period_year, r.period_month, r.status, r.total_gross,
       r.total_deductions, r.total_net, count(p.id) AS payslips
FROM payroll_runs r
LEFT JOIN payslips p ON p.run_id = r.id
GROUP BY r.id, r.period_year, r.period_month, r.status,
         r.total_gross, r.total_deductions, r.total_net
ORDER BY r.period_year, r.period_month;
