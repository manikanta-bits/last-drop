-- =====================================================================
-- LASTDROP - Step 2: tables
--
-- Six tables:
--   users          everyone who can log in (buyer, NGO, business, admin)
--   businesses     shops, with their map location and approval flag
--   products       items on sale, including all Dutch-auction fields
--   price_history  one row every time a product's price drops
--   reservations   a buyer reserving some quantity of a product
--   notifications  in-app messages for a user
--
-- Times are stored in UTC (universal time). The frontend converts them
-- to the viewer's local time.
--
-- This script DROPS and recreates the tables, so running it again gives
-- you a clean, empty database. Run 03-sample-data.sql afterwards.
-- =====================================================================

USE lastdrop;

-- Drop in reverse order of dependency (children before parents)
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS price_history;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS businesses;
DROP TABLE IF EXISTS users;

-- ---------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    email         VARCHAR(120)  NOT NULL,
    password_hash VARCHAR(100)  NOT NULL,          -- BCrypt hash, never the real password
    full_name     VARCHAR(100)  NOT NULL,
    phone         VARCHAR(20)   NULL,
    role          ENUM('BUYER', 'NGO', 'BUSINESS', 'ADMIN') NOT NULL,
    enabled       BOOLEAN       NOT NULL DEFAULT TRUE,   -- admin can disable an account
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)                  -- prevents duplicate registrations
);

-- ---------------------------------------------------------------------
-- businesses  (one business per BUSINESS user)
-- ---------------------------------------------------------------------
CREATE TABLE businesses (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    owner_id      BIGINT        NOT NULL,
    name          VARCHAR(120)  NOT NULL,
    business_type ENUM('BAKERY', 'GROCERY', 'SUPERMARKET', 'RESTAURANT', 'DISTRIBUTOR') NOT NULL,
    address       VARCHAR(255)  NOT NULL,
    phone         VARCHAR(20)   NULL,
    latitude      DECIMAL(9,6)  NOT NULL,
    longitude     DECIMAL(9,6)  NOT NULL,
    status        ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_businesses_owner (owner_id),
    KEY idx_businesses_status (status),
    KEY idx_businesses_location (latitude, longitude),
    CONSTRAINT fk_businesses_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_businesses_lat CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_businesses_lng CHECK (longitude BETWEEN -180 AND 180)
);

-- ---------------------------------------------------------------------
-- products  (each product IS its own auction - keeps things simple)
-- ---------------------------------------------------------------------
CREATE TABLE products (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    business_id           BIGINT        NOT NULL,
    name                  VARCHAR(120)  NOT NULL,
    description           VARCHAR(500)  NULL,
    category              ENUM('BAKERY', 'DAIRY', 'PRODUCE', 'MEALS', 'GROCERY', 'BEVERAGES') NOT NULL,
    image_url             VARCHAR(500)  NULL,

    quantity_total        INT           NOT NULL,     -- how many were listed
    quantity_available    INT           NOT NULL,     -- how many are left to reserve

    original_price        DECIMAL(10,2) NOT NULL,     -- normal shop price
    start_price           DECIMAL(10,2) NOT NULL,     -- auction begins here
    min_price             DECIMAL(10,2) NOT NULL,     -- price never goes below this
    current_price         DECIMAL(10,2) NOT NULL,     -- set ONLY by the backend
    drop_amount           DECIMAL(10,2) NOT NULL,     -- how much each drop removes
    drop_interval_minutes INT           NOT NULL DEFAULT 60,

    auction_start         DATETIME      NOT NULL,
    last_drop_at          DATETIME      NOT NULL,     -- time of last price change; stops double drops
    expires_at            DATETIME      NOT NULL,
    status                ENUM('ACTIVE', 'PAUSED', 'SOLD_OUT', 'EXPIRED', 'CANCELLED', 'REMOVED') NOT NULL DEFAULT 'ACTIVE',

    version               BIGINT        NOT NULL DEFAULT 0,   -- optimistic locking: stops overselling
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_products_status_expiry (status, expires_at),      -- scheduler + "active deals" queries
    KEY idx_products_business (business_id),
    KEY idx_products_category (category),
    CONSTRAINT fk_products_business FOREIGN KEY (business_id) REFERENCES businesses (id) ON DELETE CASCADE,
    CONSTRAINT chk_products_qty      CHECK (quantity_total > 0 AND quantity_available BETWEEN 0 AND quantity_total),
    CONSTRAINT chk_products_prices   CHECK (min_price > 0 AND min_price <= start_price AND start_price <= original_price),
    CONSTRAINT chk_products_current  CHECK (current_price BETWEEN min_price AND start_price),
    CONSTRAINT chk_products_drop     CHECK (drop_amount > 0 AND drop_interval_minutes > 0),
    CONSTRAINT chk_products_times    CHECK (expires_at > auction_start)
);

-- ---------------------------------------------------------------------
-- price_history  (audit trail for every automatic price drop)
-- ---------------------------------------------------------------------
CREATE TABLE price_history (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    product_id  BIGINT        NOT NULL,
    old_price   DECIMAL(10,2) NOT NULL,
    new_price   DECIMAL(10,2) NOT NULL,
    changed_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_price_history_product (product_id, changed_at),
    CONSTRAINT fk_price_history_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- reservations  (Cash on Pickup - no real payment)
-- ---------------------------------------------------------------------
CREATE TABLE reservations (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    buyer_id     BIGINT        NOT NULL,
    product_id   BIGINT        NOT NULL,
    quantity     INT           NOT NULL,
    unit_price   DECIMAL(10,2) NOT NULL,     -- price locked in at the moment of reserving
    total_price  DECIMAL(10,2) NOT NULL,
    status       ENUM('RESERVED', 'COMPLETED', 'CANCELLED', 'EXPIRED') NOT NULL DEFAULT 'RESERVED',
    pickup_by    DATETIME      NOT NULL,     -- must collect before this time (hold period)
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_reservations_buyer (buyer_id, created_at),
    KEY idx_reservations_product (product_id),
    KEY idx_reservations_status (status, pickup_by),
    CONSTRAINT fk_reservations_buyer   FOREIGN KEY (buyer_id)   REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT fk_reservations_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT chk_reservations_qty    CHECK (quantity > 0)
);

-- ---------------------------------------------------------------------
-- notifications
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    user_id     BIGINT        NOT NULL,
    type        ENUM('PRICE_DROP', 'NEW_DEAL', 'RESERVATION', 'ORDER_STATUS', 'AUCTION_EXPIRED', 'ACCOUNT') NOT NULL,
    message     VARCHAR(300)  NOT NULL,
    product_id  BIGINT        NULL,          -- optional link to the product it is about
    is_read     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notifications_user (user_id, is_read, created_at),
    CONSTRAINT fk_notifications_user    FOREIGN KEY (user_id)    REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT fk_notifications_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE SET NULL
);
