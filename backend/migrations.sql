-- One-off migrations for existing PostgreSQL databases.
--
-- Hibernate runs with ddl-auto: update, which adds new tables and columns but
-- never relaxes an existing constraint. Run these once, in order, against a
-- database that was created by an earlier version of the application.
--
-- Nothing here is needed for the H2 `dev` profile, which rebuilds the schema
-- on every start, or for a brand new PostgreSQL database.

-- Phase 4 (Campaign Generator with DM interview)
-- The interview and the initial campaign blueprint are generated BEFORE a
-- campaign exists, so generation_logs.campaign_id must allow nulls.
ALTER TABLE generation_logs ALTER COLUMN campaign_id DROP NOT NULL;

-- Phase 5 (Billing, Subscriptions & Free Tier Earnings)
-- Add user_id foreign key to campaigns
ALTER TABLE campaigns ADD COLUMN user_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE campaigns ADD CONSTRAINT fk_campaign_user FOREIGN KEY (user_id) REFERENCES users(id);

-- Drop the default constraint from the column now that it's been populated
ALTER TABLE campaigns ALTER COLUMN user_id DROP DEFAULT;


-- Phase 5 (token buckets, purchases & rollover cap)
-- Hibernate created a CHECK constraint listing the token transaction types it
-- knew about at the time, and ddl-auto: update never changes it, so the new
-- TOKEN_PURCHASE and ALLOWANCE_EXPIRED types would be rejected.
-- (users.purchased_tokens and subscriptions.pending_tier are added automatically.)
ALTER TABLE token_transactions DROP CONSTRAINT IF EXISTS token_transactions_type_check;

-- Phase 6 (AI idea generator)
-- Same issue as token_transactions above: the CHECK constraint on
-- generation_logs.type predates the new IDEA generation type.
-- (The saved_ideas table is created automatically.)
ALTER TABLE generation_logs DROP CONSTRAINT IF EXISTS generation_logs_type_check;

-- Phase 6 (shops and session planning)
-- The new SHOP location type hits the same CHECK-constraint issue.
-- (sessions.debrief is added automatically.)
ALTER TABLE locations DROP CONSTRAINT IF EXISTS locations_type_check;

-- Phase 6 (sign-in)
-- Session tokens for the login cookie; only a SHA-256 hash of each token is stored.
-- (Created automatically with ddl-auto: update; needed by hand where it is "validate".)
-- Existing plain-text passwords are hashed with BCrypt automatically on startup.
CREATE TABLE IF NOT EXISTS auth_sessions (
    id          BIGSERIAL PRIMARY KEY,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_auth_sessions_user ON auth_sessions(user_id);

-- Phase 6 (email verification, forgotten passwords, completed sessions)
-- Accounts made before verification existed count as verified.
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS completed BOOLEAN NOT NULL DEFAULT FALSE;
-- One-time email links; only a SHA-256 hash of each token is stored.
CREATE TABLE IF NOT EXISTS email_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose     VARCHAR(32)  NOT NULL,
    expires_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    used_at     TIMESTAMP(6) WITH TIME ZONE,
    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
