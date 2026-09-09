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

