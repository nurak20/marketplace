-- ================================================================
-- V2 : Seed default roles and permissions
-- ================================================================

INSERT INTO roles (name, description) VALUES
    ('ROLE_USER',  'Standard marketplace user'),
    ('ROLE_ADMIN', 'Platform administrator')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (name, description) VALUES
    ('user:read',        'View user profiles'),
    ('user:write',       'Edit own profile'),
    ('user:manage',      'Admin – manage all users'),
    ('product:read',     'View products'),
    ('product:write',    'Create / update own products'),
    ('product:manage',   'Admin – manage all products'),
    ('post:read',        'View posts'),
    ('post:write',       'Create / react to posts'),
    ('comment:write',    'Add / delete own comments'),
    ('wishlist:write',   'Add / remove wishlist items'),
    ('friend:write',     'Send / accept / decline requests'),
    ('message:write',    'Send messages'),
    ('review:write',     'Leave a review'),
    ('notification:read','View own notifications'),
    ('admin:access',     'Full admin panel access')
ON CONFLICT (name) DO NOTHING;

-- Grant all permissions to ROLE_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
CROSS  JOIN permissions p
WHERE  r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

-- Grant basic permissions to ROLE_USER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
JOIN   permissions p ON p.name IN (
    'user:read','user:write',
    'product:read','product:write',
    'post:read','post:write',
    'comment:write','wishlist:write',
    'friend:write','message:write',
    'review:write','notification:read'
)
WHERE  r.name = 'ROLE_USER'
ON CONFLICT DO NOTHING;
