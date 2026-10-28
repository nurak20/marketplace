-- ================================================================
-- V11 : Reporting and Swagger-friendly documentation
-- ================================================================

INSERT INTO permissions (name, description) VALUES
    ('report:read', 'View marketplace, engagement, and payroll reports')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')
  AND p.name = 'report:read'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.name = 'report:read'
WHERE r.name IN ('ROLE_HR', 'ROLE_MANAGER')
ON CONFLICT DO NOTHING;

-- Reports aggregate by creation month; keep those scans cheap.
CREATE INDEX IF NOT EXISTS idx_products_created_at     ON products(created_at);
CREATE INDEX IF NOT EXISTS idx_listing_posts_created_at ON listing_posts(created_at);
CREATE INDEX IF NOT EXISTS idx_comments_created_at     ON comments(created_at);
CREATE INDEX IF NOT EXISTS idx_users_created_at        ON users(created_at);
