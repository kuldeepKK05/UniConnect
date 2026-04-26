-- ================================================================
-- UniConnect PostgreSQL Schema (Primary source of truth)
-- Run with Flyway or manually
-- ================================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";        -- for LIKE/full-text perf
CREATE EXTENSION IF NOT EXISTS "btree_gist";      -- for geo exclusion

-- ────────────────────────────────────────────────────────────
-- USERS
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   TEXT         NOT NULL,
    name            VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('STUDENT','UNIVERSITY','ADMIN')),
    university_name VARCHAR(200),
    city            VARCHAR(100),
    country         VARCHAR(100),
    avatar_url      TEXT,
    verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email  ON users (email);
CREATE INDEX idx_users_role   ON users (role);
CREATE INDEX idx_users_active ON users (active) WHERE active = TRUE;

-- ────────────────────────────────────────────────────────────
-- POSTS
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS posts (
    id               UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    title            VARCHAR(300) NOT NULL,
    description      TEXT         NOT NULL,
    category         VARCHAR(30)  NOT NULL CHECK (category IN ('HACKATHON','INTERNSHIP','SUMMER_COURSE')),
    mode             VARCHAR(20)  NOT NULL CHECK (mode IN ('ON_GROUND','VIRTUAL','WFH')),
    university_name  VARCHAR(200) NOT NULL,
    city             VARCHAR(100) NOT NULL,
    state            VARCHAR(100),
    country          VARCHAR(100) NOT NULL,
    venue            VARCHAR(200),
    latitude         DOUBLE PRECISION NOT NULL DEFAULT 0,
    longitude        DOUBLE PRECISION NOT NULL DEFAULT 0,
    apply_link       VARCHAR(500) NOT NULL,
    deadline         DATE,
    image_url        TEXT,
    upvote_count     INT          NOT NULL DEFAULT 0,
    comment_count    INT          NOT NULL DEFAULT 0,
    trending_score   DOUBLE PRECISION NOT NULL DEFAULT 0,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    author_id        UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_posts_author     ON posts (author_id);
CREATE INDEX idx_posts_category   ON posts (category);
CREATE INDEX idx_posts_mode       ON posts (mode);
CREATE INDEX idx_posts_city       ON posts (LOWER(city));
CREATE INDEX idx_posts_created    ON posts (created_at DESC);
CREATE INDEX idx_posts_trending   ON posts (trending_score DESC);
CREATE INDEX idx_posts_active     ON posts (active) WHERE active = TRUE;
CREATE INDEX idx_posts_coords     ON posts (latitude, longitude);
-- GIN index for keyword search
CREATE INDEX idx_posts_title_gin  ON posts USING gin(to_tsvector('english', title));
CREATE INDEX idx_posts_desc_gin   ON posts USING gin(to_tsvector('english', description));

-- ────────────────────────────────────────────────────────────
-- COMMENTS
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS comments (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    post_id     UUID        NOT NULL REFERENCES posts(id)    ON DELETE CASCADE,
    author_id   UUID        NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    parent_id   UUID        REFERENCES comments(id)          ON DELETE SET NULL,
    content     TEXT        NOT NULL,
    like_count  INT         NOT NULL DEFAULT 0,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_post   ON comments (post_id, created_at);
CREATE INDEX idx_comments_parent ON comments (parent_id);
CREATE INDEX idx_comments_author ON comments (author_id);

-- ────────────────────────────────────────────────────────────
-- POST VOTES (upvotes)
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS post_votes (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID        NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    post_id     UUID        NOT NULL REFERENCES posts(id)  ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, post_id)
);

CREATE INDEX idx_votes_post ON post_votes (post_id);
CREATE INDEX idx_votes_user ON post_votes (user_id);

-- ────────────────────────────────────────────────────────────
-- COMMENT LIKES
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS comment_likes (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID        NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    comment_id  UUID        NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, comment_id)
);

-- ────────────────────────────────────────────────────────────
-- SAVED POSTS (bookmarks)
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS saved_posts (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id     UUID        NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    saved_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, post_id)
);

CREATE INDEX idx_saved_user ON saved_posts (user_id, saved_at DESC);
CREATE INDEX idx_saved_post ON saved_posts (post_id);

-- ────────────────────────────────────────────────────────────
-- EAV tables (ThingDB / extensible attributes)
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS eav_entities (
    entity_id   UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(50) NOT NULL,   -- 'POST', 'USER', 'UNIVERSITY'
    ref_id      UUID        NOT NULL,   -- FK to the actual record
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS eav_attributes (
    attribute_id   UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    name           VARCHAR(100) NOT NULL UNIQUE,
    data_type      VARCHAR(20)  NOT NULL DEFAULT 'TEXT',  -- TEXT, INT, BOOL, DATE
    description    TEXT
);

CREATE TABLE IF NOT EXISTS eav_values (
    id           UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_id    UUID        NOT NULL REFERENCES eav_entities(entity_id) ON DELETE CASCADE,
    attribute_id UUID        NOT NULL REFERENCES eav_attributes(attribute_id),
    value        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(entity_id, attribute_id)
);

CREATE INDEX idx_eav_entity    ON eav_values (entity_id);
CREATE INDEX idx_eav_attribute ON eav_values (attribute_id);

-- Seed common EAV attributes
INSERT INTO eav_attributes (name, data_type, description) VALUES
    ('prize_pool',        'TEXT', 'Total prize pool for hackathon'),
    ('team_size',         'INT',  'Max team size allowed'),
    ('stipend_amount',    'TEXT', 'Monthly stipend for internship'),
    ('duration_weeks',    'INT',  'Duration in weeks'),
    ('eligibility',       'TEXT', 'Eligibility criteria'),
    ('registration_fee',  'TEXT', 'Registration fee if any'),
    ('certificate',       'BOOL', 'Whether certificate is provided'),
    ('remote_option',     'BOOL', 'Partial remote allowed'),
    ('contact_email',     'TEXT', 'Direct contact email')
ON CONFLICT (name) DO NOTHING;

-- ────────────────────────────────────────────────────────────
-- Trigger: auto-update updated_at
-- ────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated  BEFORE UPDATE ON users  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_posts_updated  BEFORE UPDATE ON posts  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
