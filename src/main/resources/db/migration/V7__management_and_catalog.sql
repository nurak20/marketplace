ALTER TABLE users
    ADD COLUMN IF NOT EXISTS can_sell BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS marketplace_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO marketplace_categories (name) VALUES
    ('Electronics'),
    ('Fashion'),
    ('Home & Furniture'),
    ('Vehicles'),
    ('Books & Hobbies')
ON CONFLICT (name) DO NOTHING;

INSERT INTO marketplace_categories (name)
SELECT DISTINCT ON (LOWER(BTRIM(p.category))) BTRIM(p.category)
FROM products p
WHERE p.category IS NOT NULL AND BTRIM(p.category) <> ''
  AND NOT EXISTS (
      SELECT 1 FROM marketplace_categories c
      WHERE LOWER(c.name) = LOWER(BTRIM(p.category))
  )
ORDER BY LOWER(BTRIM(p.category)), BTRIM(p.category)
ON CONFLICT (name) DO NOTHING;

CREATE TABLE IF NOT EXISTS marketplace_conditions (
    code VARCHAR(30) PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO marketplace_conditions (code) VALUES
    ('NEW'), ('LIKE_NEW'), ('GOOD'), ('FAIR'), ('POOR')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (name, description) VALUES
    ('management:access', 'Open the staff management workspace'),
    ('category:manage', 'Create and update marketplace categories'),
    ('condition:manage', 'Enable or disable product conditions'),
    ('post:moderate', 'Remove or restore marketplace posts')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name, description) VALUES
    ('ROLE_HR', 'Human resources staff'),
    ('ROLE_EMPLOYEE', 'Staff member with read-only management access'),
    ('ROLE_MANAGER', 'Marketplace operations manager'),
    ('ROLE_SUPER_ADMIN', 'Full platform administrator')
ON CONFLICT (name) DO NOTHING;

-- Legacy administrators and super administrators keep full control.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.name IN ('management:access', 'user:manage')
WHERE r.name = 'ROLE_HR'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.name = 'management:access'
WHERE r.name = 'ROLE_EMPLOYEE'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.name IN (
    'management:access', 'user:manage', 'category:manage',
    'condition:manage', 'post:moderate'
)
WHERE r.name = 'ROLE_MANAGER'
ON CONFLICT DO NOTHING;
