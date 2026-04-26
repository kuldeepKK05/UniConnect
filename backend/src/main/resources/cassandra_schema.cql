-- ================================================================
-- UniConnect Cassandra Schema (Feed Optimization Layer)
-- Keyspace: uniconnect
-- ================================================================

CREATE KEYSPACE IF NOT EXISTS uniconnect
    WITH replication = {
        'class': 'NetworkTopologyStrategy',
        'datacenter1': 3
    }
    AND durable_writes = true;

USE uniconnect;

-- ────────────────────────────────────────────────────────────
-- posts_by_location
-- Partition by geo bucket (country + city prefix), cluster by created_at DESC
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS posts_by_location (
    country         TEXT,
    city            TEXT,
    created_at      TIMESTAMP,
    post_id         UUID,
    title           TEXT,
    university_name TEXT,
    category        TEXT,
    mode            TEXT,
    latitude        DOUBLE,
    longitude       DOUBLE,
    upvote_count    INT,
    comment_count   INT,
    trending_score  DOUBLE,
    deadline        DATE,
    image_url       TEXT,
    PRIMARY KEY ((country, city), created_at, post_id)
) WITH CLUSTERING ORDER BY (created_at DESC, post_id ASC)
  AND default_time_to_live = 2592000  -- 30 days TTL (refreshed on update)
  AND gc_grace_seconds = 864000;

-- ────────────────────────────────────────────────────────────
-- posts_by_trending
-- Partition by date bucket (yyyy-mm-dd), cluster by trending_score DESC
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS posts_by_trending (
    date_bucket     TEXT,           -- 'global' or 'YYYY-MM' for time-window trending
    trending_score  DOUBLE,
    post_id         UUID,
    title           TEXT,
    university_name TEXT,
    category        TEXT,
    mode            TEXT,
    city            TEXT,
    country         TEXT,
    latitude        DOUBLE,
    longitude       DOUBLE,
    upvote_count    INT,
    comment_count   INT,
    created_at      TIMESTAMP,
    deadline        DATE,
    image_url       TEXT,
    PRIMARY KEY ((date_bucket), trending_score, post_id)
) WITH CLUSTERING ORDER BY (trending_score DESC, post_id ASC)
  AND default_time_to_live = 604800  -- 7 days
  AND gc_grace_seconds = 86400;

-- ────────────────────────────────────────────────────────────
-- posts_by_user  (saved / posted)
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS posts_by_user (
    user_id         UUID,
    list_type       TEXT,           -- 'SAVED' | 'POSTED'
    created_at      TIMESTAMP,
    post_id         UUID,
    title           TEXT,
    university_name TEXT,
    category        TEXT,
    mode            TEXT,
    city            TEXT,
    country         TEXT,
    image_url       TEXT,
    PRIMARY KEY ((user_id, list_type), created_at, post_id)
) WITH CLUSTERING ORDER BY (created_at DESC, post_id ASC)
  AND gc_grace_seconds = 864000;

-- ────────────────────────────────────────────────────────────
-- posts_by_category
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS posts_by_category (
    category        TEXT,
    created_at      TIMESTAMP,
    post_id         UUID,
    title           TEXT,
    university_name TEXT,
    mode            TEXT,
    city            TEXT,
    country         TEXT,
    latitude        DOUBLE,
    longitude       DOUBLE,
    upvote_count    INT,
    trending_score  DOUBLE,
    deadline        DATE,
    image_url       TEXT,
    PRIMARY KEY ((category), created_at, post_id)
) WITH CLUSTERING ORDER BY (created_at DESC, post_id ASC)
  AND default_time_to_live = 2592000
  AND gc_grace_seconds = 864000;

-- ────────────────────────────────────────────────────────────
-- feed_cursor_state  (tracks pagination state server-side if needed)
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS feed_cursor_state (
    session_id      UUID,
    query_hash      TEXT,
    last_seen_id    UUID,
    last_seen_ts    TIMESTAMP,
    created_at      TIMESTAMP,
    PRIMARY KEY ((session_id), query_hash)
) WITH default_time_to_live = 3600;  -- 1 hour TTL

-- ────────────────────────────────────────────────────────────
-- Materialized Views
-- ────────────────────────────────────────────────────────────

-- Virtual/global posts view (mode = VIRTUAL or WFH)
CREATE MATERIALIZED VIEW IF NOT EXISTS posts_virtual AS
    SELECT * FROM posts_by_location
    WHERE mode IN ('VIRTUAL', 'WFH')
      AND country IS NOT NULL
      AND city    IS NOT NULL
      AND created_at IS NOT NULL
      AND post_id IS NOT NULL
    PRIMARY KEY ((mode), created_at, post_id)
WITH CLUSTERING ORDER BY (created_at DESC, post_id ASC);
