-- ================================================================
-- V9 : Staff payroll
-- ================================================================

DO $$ BEGIN
    CREATE TYPE employment_status AS ENUM ('ACTIVE','ON_LEAVE','TERMINATED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE payroll_run_status AS ENUM ('DRAFT','APPROVED','PAID','CANCELLED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

ALTER TYPE notif_type ADD VALUE IF NOT EXISTS 'PAYROLL';

CREATE TABLE IF NOT EXISTS employee_profiles (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT        NOT NULL REFERENCES users(id),
    employee_code  VARCHAR(30)   NOT NULL UNIQUE,
    job_title      VARCHAR(100),
    department     VARCHAR(100),
    base_salary    NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (base_salary >= 0),
    allowance      NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (allowance >= 0),
    currency       VARCHAR(3)    NOT NULL DEFAULT 'USD',
    hire_date      DATE,
    bank_name      VARCHAR(100),
    bank_account   VARCHAR(50),
    status         employment_status NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT employee_profiles_user_unique UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_employee_profiles_status     ON employee_profiles(status);
CREATE INDEX IF NOT EXISTS idx_employee_profiles_department ON employee_profiles(department);

CREATE TABLE IF NOT EXISTS payroll_runs (
    id                BIGSERIAL PRIMARY KEY,
    period_year       INTEGER       NOT NULL CHECK (period_year BETWEEN 2000 AND 2100),
    period_month      INTEGER       NOT NULL CHECK (period_month BETWEEN 1 AND 12),
    currency          VARCHAR(3)    NOT NULL DEFAULT 'USD',
    status            payroll_run_status NOT NULL DEFAULT 'DRAFT',
    notes             TEXT,
    total_gross       NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total_gross >= 0),
    total_deductions  NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total_deductions >= 0),
    total_net         NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total_net >= 0),
    created_by        BIGINT        NOT NULL REFERENCES users(id),
    approved_by       BIGINT        REFERENCES users(id),
    approved_at       TIMESTAMPTZ,
    paid_at           TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT payroll_runs_period_unique UNIQUE (period_year, period_month)
);

CREATE TABLE IF NOT EXISTS payslips (
    id                BIGSERIAL PRIMARY KEY,
    run_id            BIGINT        NOT NULL REFERENCES payroll_runs(id) ON DELETE CASCADE,
    user_id           BIGINT        NOT NULL REFERENCES users(id),
    employee_code     VARCHAR(30)   NOT NULL,
    employee_name     VARCHAR(100)  NOT NULL,
    job_title         VARCHAR(100),
    department        VARCHAR(100),
    base_salary       NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (base_salary >= 0),
    allowance         NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (allowance >= 0),
    bonus             NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (bonus >= 0),
    overtime          NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (overtime >= 0),
    tax               NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (tax >= 0),
    other_deductions  NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (other_deductions >= 0),
    gross_pay         NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (gross_pay >= 0),
    deductions_total  NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (deductions_total >= 0),
    net_pay           NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (net_pay >= 0),
    currency          VARCHAR(3)    NOT NULL DEFAULT 'USD',
    notes             TEXT,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT payslips_run_employee_unique UNIQUE (run_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_payslips_run  ON payslips(run_id);
CREATE INDEX IF NOT EXISTS idx_payslips_user ON payslips(user_id);

INSERT INTO permissions (name, description) VALUES
    ('payroll:read',    'View employee compensation, payroll runs, and payslips'),
    ('payroll:manage',  'Manage employee profiles and payroll runs'),
    ('payroll:approve', 'Approve payroll runs and mark them paid')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')
  AND p.name LIKE 'payroll:%'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.name IN ('payroll:read', 'payroll:manage')
WHERE r.name = 'ROLE_HR'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.name IN ('payroll:read', 'payroll:approve')
WHERE r.name = 'ROLE_MANAGER'
ON CONFLICT DO NOTHING;
