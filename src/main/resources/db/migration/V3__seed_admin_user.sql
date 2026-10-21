INSERT INTO users (username, email, password, full_name, is_active)
VALUES ('admin', 'admin@example.com', crypt('12345678', gen_salt('bf', 10)), 'Super Admin', TRUE)
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'admin'
  AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;
