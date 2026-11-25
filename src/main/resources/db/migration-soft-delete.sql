-- ============================================================================
-- Soft delete: adds a deleted_at column to every main table of the EXISTING database.
-- Run ONCE on the real database before starting the new version of the app:
--     mysql -u root -p bloggingPlatform < src/main/resources/db/migration-soft-delete.sql
--
-- deleted_at IS NULL      -> the row is active (shown by the API)
-- deleted_at = a date     -> the row was deleted at that time (hidden, but kept in the table)
-- Existing rows get NULL, so nothing disappears when this script runs.
-- ============================================================================

ALTER TABLE users    ADD COLUMN deleted_at DATETIME NULL DEFAULT NULL;
ALTER TABLE posts    ADD COLUMN deleted_at DATETIME NULL DEFAULT NULL;
ALTER TABLE tags     ADD COLUMN deleted_at DATETIME NULL DEFAULT NULL;
ALTER TABLE comments ADD COLUMN deleted_at DATETIME NULL DEFAULT NULL;
ALTER TABLE reviews  ADD COLUMN deleted_at DATETIME NULL DEFAULT NULL;

-- Undo (only if you go back to the old version of the app; deleted rows become visible again):
-- ALTER TABLE users DROP COLUMN deleted_at;  ALTER TABLE posts DROP COLUMN deleted_at;
-- ALTER TABLE tags DROP COLUMN deleted_at;   ALTER TABLE comments DROP COLUMN deleted_at;
-- ALTER TABLE reviews DROP COLUMN deleted_at;
