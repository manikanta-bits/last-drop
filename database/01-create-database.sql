-- =====================================================================
-- LASTDROP - Step 1: create the database and an app user
--
-- Run this ONCE as the MySQL "root" user.
--
-- Why a separate user? The backend should not log in as root (root can
-- do anything, including deleting every database). The "lastdrop" user
-- can only touch the "lastdrop" database.
--
-- The password below is for LOCAL DEVELOPMENT ONLY. On the real server
-- (Phase 8) you will use a different, secret password supplied through
-- an environment variable.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS lastdrop
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'lastdrop'@'localhost' IDENTIFIED BY 'lastdrop_dev';

GRANT ALL PRIVILEGES ON lastdrop.* TO 'lastdrop'@'localhost';

FLUSH PRIVILEGES;
