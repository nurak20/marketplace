-- ================================================================
-- V1 : Full marketplace schema
-- ================================================================

-- EXTENSION
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ──────────────────── ENUM TYPES ─────────────────────────────────
DO $$ BEGIN
    CREATE TYPE product_status AS ENUM ('AVAILABLE','SOLD','RESERVED','DELETED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN
    CREATE TYPE condition_type AS ENUM ('NEW','LIKE_NEW','GOOD','FAIR','POOR');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN
    CREATE TYPE friend_status AS ENUM ('PENDING','ACCEPTED','DECLINED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN
    CREATE TYPE notif_type AS ENUM ('COMMENT','MESSAGE','FRIEND_REQUEST','REVIEW','LIKE');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN
    CREATE TYPE reaction_type AS ENUM ('LIKE','LOVE','HAHA','WOW','SAD','ANGRY');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ──────────────────── ROLES & PERMISSIONS ─────────────────────────
CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS permissions (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- ──────────────────── USERS ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(100),
    avatar_url  VARCHAR(500),
    bio         TEXT,
    location    VARCHAR(100),
    phone       VARCHAR(20),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id)  ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ──────────────────── PRODUCTS ────────────────────────────────────
CREATE TABLE IF NOT EXISTS products (
    id            BIGSERIAL PRIMARY KEY,
    seller_id     BIGINT          NOT NULL REFERENCES users(id),
    title         VARCHAR(200)    NOT NULL,
    description   TEXT,
    price         NUMERIC(12,2)   NOT NULL,
    category      VARCHAR(100),
    condition     condition_type  NOT NULL DEFAULT 'GOOD',
    location      VARCHAR(100),
    status        product_status  NOT NULL DEFAULT 'AVAILABLE',
    search_vector TSVECTOR,
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_products_search ON products USING GIN(search_vector);
CREATE INDEX IF NOT EXISTS idx_products_seller  ON products(seller_id);
CREATE INDEX IF NOT EXISTS idx_products_status  ON products(status);

CREATE OR REPLACE FUNCTION products_search_vector_update() RETURNS TRIGGER AS $$
BEGIN
  NEW.search_vector :=
    setweight(to_tsvector('english', COALESCE(NEW.title, '')),       'A') ||
    setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B') ||
    setweight(to_tsvector('english', COALESCE(NEW.category, '')),    'C');
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_products_search ON products;
CREATE TRIGGER trg_products_search
BEFORE INSERT OR UPDATE ON products
FOR EACH ROW EXECUTE FUNCTION products_search_vector_update();

CREATE TABLE IF NOT EXISTS product_images (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT       NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_url  VARCHAR(500) NOT NULL,
    is_primary BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ──────────────────── LISTING POSTS ──────────────────────────────
CREATE TABLE IF NOT EXISTS listing_posts (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT  NOT NULL REFERENCES users(id),
    content    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS listing_post_items (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT NOT NULL REFERENCES listing_posts(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    UNIQUE (post_id, product_id)
);

CREATE TABLE IF NOT EXISTS post_reactions (
    id            BIGSERIAL PRIMARY KEY,
    post_id       BIGINT        NOT NULL REFERENCES listing_posts(id) ON DELETE CASCADE,
    user_id       BIGINT        NOT NULL REFERENCES users(id),
    reaction_type reaction_type NOT NULL DEFAULT 'LIKE',
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (post_id, user_id)
);

-- ──────────────────── COMMENTS ────────────────────────────────────
CREATE TABLE IF NOT EXISTS comments (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT NOT NULL REFERENCES listing_posts(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    content    TEXT   NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- ──────────────────── WISHLISTS ───────────────────────────────────
CREATE TABLE IF NOT EXISTS wishlists (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, product_id)
);

-- ──────────────────── FRIENDSHIPS ─────────────────────────────────
CREATE TABLE IF NOT EXISTS friendships (
    id           BIGSERIAL PRIMARY KEY,
    requester_id BIGINT        NOT NULL REFERENCES users(id),
    addressee_id BIGINT        NOT NULL REFERENCES users(id),
    status       friend_status NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (requester_id, addressee_id),
    CHECK (requester_id <> addressee_id)
);

-- ──────────────────── MESSAGES ────────────────────────────────────
CREATE TABLE IF NOT EXISTS messages (
    id          BIGSERIAL PRIMARY KEY,
    sender_id   BIGINT NOT NULL REFERENCES users(id),
    receiver_id BIGINT NOT NULL REFERENCES users(id),
    product_id  BIGINT REFERENCES products(id),
    content     TEXT   NOT NULL,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (sender_id <> receiver_id)
);

-- ──────────────────── NOTIFICATIONS ──────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT     NOT NULL REFERENCES users(id),
    type       notif_type NOT NULL,
    message    TEXT       NOT NULL,
    ref_id     BIGINT,
    is_read    BOOLEAN    NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ──────────────────── REVIEWS ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS reviews (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT   NOT NULL REFERENCES products(id),
    buyer_id   BIGINT   NOT NULL REFERENCES users(id),
    seller_id  BIGINT   NOT NULL REFERENCES users(id),
    rating     SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (product_id, buyer_id)
);
