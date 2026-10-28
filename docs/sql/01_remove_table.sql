-- ============================================================================
-- 01 · remove_table
-- ----------------------------------------------------------------------------
-- DESTRUCTIVE SCRIPT — DEVELOPMENT DATABASES ONLY.
--
-- Drops every table in the public schema (including flyway_schema_history and
-- every payroll table from V9), plus the application's enum types, leftover
-- sequences, views, and the products search trigger function.
-- The pgcrypto / pg_trgm extensions are kept so password hashing still works.
--
-- Because flyway_schema_history is dropped, starting the app afterwards
-- re-runs migrations V1..V11 from scratch.
--
-- Workflow:
--   1. Run this script.
--   2. Start the Spring Boot app once (Flyway rebuilds schema + base seeds).
--   3. Run 02_init_data.sql.
--
-- Usage in pgAdmin: connect to your dev database, open Query Tool,
-- paste this file and execute (F5).
-- ============================================================================

BEGIN;

-- 1) Views (none today, kept for a complete reset) ---------------------------
DO $$
DECLARE v RECORD;
BEGIN
    FOR v IN SELECT viewname FROM pg_views WHERE schemaname = 'public' LOOP
        EXECUTE format('DROP VIEW IF EXISTS public.%I CASCADE', v.viewname);
        RAISE NOTICE 'dropped view public.%', v.viewname;
    END LOOP;

    FOR v IN SELECT matviewname FROM pg_matviews WHERE schemaname = 'public' LOOP
        EXECUTE format('DROP MATERIALIZED VIEW IF EXISTS public.%I CASCADE', v.matviewname);
        RAISE NOTICE 'dropped materialized view public.%', v.matviewname;
    END LOOP;
END $$;

-- 2) Tables (CASCADE removes FKs, indexes, triggers, owned sequences) -------
--    Includes flyway_schema_history on purpose: the next app start replays
--    every migration instead of believing the schema is already applied.
DO $$
DECLARE t RECORD;
BEGIN
    FOR t IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' LOOP
        EXECUTE format('DROP TABLE IF EXISTS public.%I CASCADE', t.tablename);
        RAISE NOTICE 'dropped table public.%', t.tablename;
    END LOOP;
END $$;

-- 3) Leftover sequences ------------------------------------------------------
DO $$
DECLARE s RECORD;
BEGIN
    FOR s IN SELECT sequence_name FROM information_schema.sequences
             WHERE sequence_schema = 'public' LOOP
        EXECUTE format('DROP SEQUENCE IF EXISTS public.%I CASCADE', s.sequence_name);
        RAISE NOTICE 'dropped sequence public.%', s.sequence_name;
    END LOOP;
END $$;

-- 4) Enum types (skip ones owned by an extension) ----------------------------
DO $$
DECLARE t RECORD;
BEGIN
    FOR t IN
        SELECT ty.typname
        FROM pg_type ty
        JOIN pg_namespace n ON n.oid = ty.typnamespace
        WHERE n.nspname = 'public'
          AND ty.typtype = 'e'
          AND NOT EXISTS (
              SELECT 1 FROM pg_depend d
              WHERE d.objid = ty.oid AND d.deptype = 'e'
          )
    LOOP
        EXECUTE format('DROP TYPE IF EXISTS public.%I CASCADE', t.typname);
        RAISE NOTICE 'dropped type public.%', t.typname;
    END LOOP;
END $$;

-- 5) Helper functions --------------------------------------------------------
DROP FUNCTION IF EXISTS public.products_search_vector_update() CASCADE;

COMMIT;

-- Verification: both statements should return 0 rows.
SELECT count(*) AS remaining_tables FROM pg_tables WHERE schemaname = 'public';
SELECT count(*) AS remaining_enum_types
FROM pg_type ty
JOIN pg_namespace n ON n.oid = ty.typnamespace
WHERE n.nspname = 'public' AND ty.typtype = 'e';
