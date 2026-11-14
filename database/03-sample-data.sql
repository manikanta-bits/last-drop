-- =====================================================================
-- LASTDROP - Step 3: sample (demo) data
--
-- Every demo account uses the password:  Demo@123
-- (stored below as a BCrypt hash, exactly like real users)
--
-- All times are calculated from "now" when you run this file, so the
-- auctions are always live. Re-run 02-schema.sql then this file to
-- reset the demo at any time.
--
-- Locations are around Hyderabad, India.
-- =====================================================================

USE lastdrop;
SET NAMES utf8mb4;          -- so the rupee sign is stored correctly
SET time_zone = '+00:00';   -- this session works in UTC
SET @now = UTC_TIMESTAMP();
SET @pw  = '$2a$10$oLVrxFQ2Ig6.aYkJiSE2R.v/EIcovKv76vYzjq5.bBt9KJY9LlWHu';  -- "Demo@123"

-- ---------------------------------------------------------------------
-- Users
-- ---------------------------------------------------------------------
INSERT INTO users (id, email, password_hash, full_name, phone, role) VALUES
 (1,  'admin@lastdrop.demo',      @pw, 'LASTDROP Admin',        '9000000001', 'ADMIN'),
 (2,  'buyer@lastdrop.demo',      @pw, 'Priya Sharma',          '9000000002', 'BUYER'),
 (3,  'buyer2@lastdrop.demo',     @pw, 'Rahul Verma',           '9000000003', 'BUYER'),
 (4,  'ngo@lastdrop.demo',        @pw, 'Annapurna Food Bank',   '9000000004', 'NGO'),
 (5,  'ngo2@lastdrop.demo',       @pw, 'Seva Meals Trust',      '9000000005', 'NGO'),
 (6,  'business1@lastdrop.demo',  @pw, 'Arjun Reddy',           '9000000006', 'BUSINESS'),
 (7,  'business2@lastdrop.demo',  @pw, 'Meera Iyer',            '9000000007', 'BUSINESS'),
 (8,  'business3@lastdrop.demo',  @pw, 'Kiran Rao',             '9000000008', 'BUSINESS'),
 (9,  'business4@lastdrop.demo',  @pw, 'Sana Khan',             '9000000009', 'BUSINESS'),
 (10, 'business5@lastdrop.demo',  @pw, 'Vikram Patel',          '9000000010', 'BUSINESS'),
 (11, 'business6@lastdrop.demo',  @pw, 'Lakshmi Naidu',         '9000000011', 'BUSINESS'),
 (12, 'business7@lastdrop.demo',  @pw, 'Rohan Mehta',           '9000000012', 'BUSINESS'),
 (13, 'business8@lastdrop.demo',  @pw, 'Fatima Ali',            '9000000013', 'BUSINESS'),
 (14, 'business9@lastdrop.demo',  @pw, 'Suresh Kumar',          '9000000014', 'BUSINESS'),
 (15, 'business10@lastdrop.demo', @pw, 'Anjali Desai',          '9000000015', 'BUSINESS'),
 (16, 'business11@lastdrop.demo', @pw, 'Naveen Joshi',          '9000000016', 'BUSINESS');

-- ---------------------------------------------------------------------
-- Businesses: 5 bakeries, 3 grocery stores, 2 restaurants (approved)
--             + 1 bakery waiting for admin approval
-- ---------------------------------------------------------------------
INSERT INTO businesses (id, owner_id, name, business_type, address, phone, latitude, longitude, status) VALUES
 (1,  6,  'Golden Crust Bakery',    'BAKERY',     'Road No. 36, Madhapur, Hyderabad',          '9100000001', 17.448300, 78.391500, 'APPROVED'),
 (2,  7,  'Sunrise Bakehouse',      'BAKERY',     'Road No. 10, Jubilee Hills, Hyderabad',     '9100000002', 17.432600, 78.407100, 'APPROVED'),
 (3,  8,  'The Daily Loaf',         'BAKERY',     'Road No. 12, Banjara Hills, Hyderabad',     '9100000003', 17.415600, 78.434700, 'APPROVED'),
 (4,  9,  'Sweet Oven Patisserie',  'BAKERY',     'DLF Road, Gachibowli, Hyderabad',           '9100000004', 17.440100, 78.348900, 'APPROVED'),
 (5,  10, 'Crumbs & Co. Bakery',    'BAKERY',     'Botanical Garden Road, Kondapur, Hyderabad','9100000005', 17.469800, 78.357800, 'APPROVED'),
 (6,  11, 'GreenBasket Grocery',    'GROCERY',    'Main Road, Ameerpet, Hyderabad',            '9100000006', 17.437500, 78.448300, 'APPROVED'),
 (7,  12, 'FreshMart Grocery',      'GROCERY',    'SP Road, Begumpet, Hyderabad',              '9100000007', 17.444700, 78.466400, 'APPROVED'),
 (8,  13, 'Nature''s Pantry',       'GROCERY',    'KPHB Colony, Kukatpally, Hyderabad',        '9100000008', 17.484900, 78.413800, 'APPROVED'),
 (9,  14, 'Spice Route Kitchen',    'RESTAURANT', 'Raj Bhavan Road, Somajiguda, Hyderabad',    '9100000009', 17.423900, 78.459400, 'APPROVED'),
 (10, 15, 'Tiffin Junction',        'RESTAURANT', 'Panchavati Colony, Manikonda, Hyderabad',   '9100000010', 17.405000, 78.386600, 'APPROVED'),
 (11, 16, 'Fresh Crumbs Bakery',    'BAKERY',     'MG Road, Secunderabad, Hyderabad',          '9100000011', 17.439900, 78.498300, 'PENDING');

-- ---------------------------------------------------------------------
-- Products (each one is a Dutch auction)
-- Columns: business, name, description, category,
--          qty total, qty available,
--          original, start, minimum, current, drop amount, interval (min),
--          auction start, last drop, expires, status
-- ---------------------------------------------------------------------
INSERT INTO products (id, business_id, name, description, category,
                      quantity_total, quantity_available,
                      original_price, start_price, min_price, current_price, drop_amount, drop_interval_minutes,
                      auction_start, last_drop_at, expires_at, status) VALUES
 -- Golden Crust Bakery (already dropped twice: 350 -> 325 -> 300)
 (1,  1, 'Fresh Bakery Cake (1 kg)', 'Chocolate truffle cake baked this morning.', 'BAKERY',
       3, 3,    500.00, 350.00, 100.00, 300.00, 25.00, 60,
       @now - INTERVAL 130 MINUTE, @now - INTERVAL 10 MINUTE, @now + INTERVAL 6 HOUR, 'ACTIVE'),
 (2,  1, 'Butter Croissants (pack of 6)', 'Flaky all-butter croissants.', 'BAKERY',
      10, 10,   240.00, 180.00,  60.00, 180.00, 15.00, 60,
       @now, @now, @now + INTERVAL 5 HOUR, 'ACTIVE'),
 -- Sunrise Bakehouse
 (3,  2, 'Whole Wheat Bread Loaf', '400 g sliced whole wheat loaf.', 'BAKERY',
      15, 15,    60.00,  45.00,  15.00,  45.00,  5.00, 60,
       @now, @now, @now + INTERVAL 8 HOUR, 'ACTIVE'),
 (4,  2, 'Blueberry Muffins (4 pack)', 'Soft muffins with real blueberries.', 'BAKERY',
       8, 8,    200.00, 150.00,  50.00, 140.00, 10.00, 30,
       @now - INTERVAL 40 MINUTE, @now - INTERVAL 10 MINUTE, @now + INTERVAL 3 HOUR, 'ACTIVE'),
 -- The Daily Loaf
 (5,  3, 'Garlic Focaccia', 'Rosemary and garlic focaccia, half tray.', 'BAKERY',
       6, 6,    180.00, 130.00,  40.00, 130.00, 10.00, 60,
       @now, @now, @now + INTERVAL 4 HOUR, 'ACTIVE'),
 (6,  3, 'Cinnamon Rolls (pack of 6)', 'Glazed cinnamon rolls.', 'BAKERY',
       5, 4,    270.00, 200.00,  70.00, 200.00, 15.00, 60,
       @now, @now, @now + INTERVAL 2 HOUR, 'ACTIVE'),
 -- Sweet Oven Patisserie
 (7,  4, 'Red Velvet Pastries (4)', 'Cream cheese frosted pastries.', 'BAKERY',
      12, 12,   320.00, 240.00,  80.00, 240.00, 20.00, 120,
       @now, @now, @now + INTERVAL 10 HOUR, 'ACTIVE'),
 (8,  4, 'Mixed Fruit Tart', 'Large tart with seasonal fruit. Expiring very soon!', 'BAKERY',
       4, 4,    450.00, 320.00, 120.00, 320.00, 25.00, 60,
       @now, @now, @now + INTERVAL 50 MINUTE, 'ACTIVE'),
 -- Crumbs & Co. Bakery
 (9,  5, 'Multigrain Buns (8)', 'Soft multigrain burger buns.', 'BAKERY',
      10, 10,   120.00,  90.00,  30.00,  90.00,  5.00, 60,
       @now, @now, @now + INTERVAL 7 HOUR, 'ACTIVE'),
 (10, 5, 'Veg Puffs (6)', 'Spicy vegetable puffs.', 'BAKERY',
      20, 20,   150.00, 110.00,  40.00, 110.00, 10.00, 30,
       @now, @now, @now + INTERVAL 3 HOUR, 'ACTIVE'),
 -- GreenBasket Grocery
 (11, 6, 'Bananas (1 dozen)', 'Ripe Robusta bananas.', 'PRODUCE',
      12, 12,    70.00,  50.00,  20.00,  50.00,  5.00, 120,
       @now, @now, @now + INTERVAL 20 HOUR, 'ACTIVE'),
 (12, 6, 'Fresh Paneer (200 g)', 'Soft cottage cheese, best before tonight.', 'DAIRY',
      15, 15,   100.00,  80.00,  35.00,  80.00,  5.00, 60,
       @now, @now, @now + INTERVAL 12 HOUR, 'ACTIVE'),
 (13, 6, 'Toned Milk 1 L (pack of 2)', 'Pasteurised toned milk.', 'DAIRY',
      25, 25,    66.00,  55.00,  25.00,  55.00,  3.00, 60,
       @now, @now, @now + INTERVAL 9 HOUR, 'ACTIVE'),
 -- FreshMart Grocery
 (14, 7, 'Mixed Salad Greens (250 g)', 'Lettuce, rocket and spinach mix.', 'PRODUCE',
      10, 10,   120.00,  85.00,  30.00,  85.00,  5.00, 60,
       @now, @now, @now + INTERVAL 6 HOUR, 'ACTIVE'),
 (15, 7, 'Greek Yogurt (400 g)', 'Thick unsweetened Greek yogurt.', 'DAIRY',
       8, 8,    180.00, 130.00,  50.00, 130.00, 10.00, 120,
       @now, @now, @now + INTERVAL 18 HOUR, 'ACTIVE'),
 (16, 7, 'Cold-Pressed Orange Juice (1 L)', 'No added sugar.', 'BEVERAGES',
       6, 6,    220.00, 160.00,  60.00, 160.00, 10.00, 60,
       @now, @now, @now + INTERVAL 14 HOUR, 'ACTIVE'),
 -- Nature's Pantry
 (17, 8, 'Tomatoes (1 kg)', 'Very ripe tomatoes, great for curries.', 'PRODUCE',
      30, 30,    40.00,  30.00,  10.00,  30.00,  2.00, 120,
       @now, @now, @now + INTERVAL 22 HOUR, 'ACTIVE'),
 (18, 8, 'Brown Eggs (1 dozen)', 'Farm eggs nearing best-before date.', 'GROCERY',
      12, 12,   110.00,  85.00,  40.00,  85.00,  5.00, 120,
       @now, @now, @now + INTERVAL 30 HOUR, 'ACTIVE'),
 -- Spice Route Kitchen
 (19, 9, 'Veg Biryani (family pack)', 'Serves 3-4. Cooked at lunch today.', 'MEALS',
       8, 8,    400.00, 260.00,  90.00, 260.00, 20.00, 60,
       @now, @now, @now + INTERVAL 4 HOUR, 'ACTIVE'),
 (20, 9, 'Paneer Butter Masala + 4 Rotis', 'Freshly prepared combo meal.', 'MEALS',
       6, 6,    320.00, 220.00,  80.00, 180.00, 20.00, 30,
       @now - INTERVAL 70 MINUTE, @now - INTERVAL 10 MINUTE, @now + INTERVAL 2 HOUR, 'ACTIVE'),
 -- Tiffin Junction
 (21, 10, 'Idli-Vada Combo (4 plates)', 'With sambar and chutney.', 'MEALS',
      10, 10,   240.00, 160.00,  60.00, 160.00, 10.00, 60,
       @now, @now, @now + INTERVAL 3 HOUR, 'ACTIVE'),
 (22, 10, 'Masala Dosa Batter (1 kg)', 'Fermented batter, use within a day.', 'GROCERY',
      12, 12,    90.00,  65.00,  25.00,  65.00,  5.00, 60,
       @now, @now, @now + INTERVAL 16 HOUR, 'ACTIVE'),
 -- Finished auctions (for history and dashboard statistics)
 (23, 3, 'Sourdough Loaf', 'Expired before anyone reserved it.', 'BAKERY',
       4, 4,    220.00, 150.00,  50.00,  50.00, 25.00, 60,
       @now - INTERVAL 26 HOUR, @now - INTERVAL 22 HOUR, @now - INTERVAL 2 HOUR, 'EXPIRED'),
 (24, 8, 'Spinach Bunch', 'Fresh spinach - fully rescued!', 'PRODUCE',
      10, 0,     30.00,  25.00,   8.00,  21.00,  2.00, 60,
       @now - INTERVAL 3 HOUR, @now - INTERVAL 1 HOUR, @now + INTERVAL 5 HOUR, 'SOLD_OUT');

-- ---------------------------------------------------------------------
-- Price history for products that have already dropped
-- ---------------------------------------------------------------------
INSERT INTO price_history (product_id, old_price, new_price, changed_at) VALUES
 (1,  350.00, 325.00, @now - INTERVAL 70 MINUTE),
 (1,  325.00, 300.00, @now - INTERVAL 10 MINUTE),
 (4,  150.00, 140.00, @now - INTERVAL 10 MINUTE),
 (20, 220.00, 200.00, @now - INTERVAL 40 MINUTE),
 (20, 200.00, 180.00, @now - INTERVAL 10 MINUTE),
 (23, 150.00, 125.00, @now - INTERVAL 25 HOUR),
 (23, 125.00, 100.00, @now - INTERVAL 24 HOUR),
 (23, 100.00,  75.00, @now - INTERVAL 23 HOUR),
 (23,  75.00,  50.00, @now - INTERVAL 22 HOUR),
 (24,  25.00,  23.00, @now - INTERVAL 2 HOUR),
 (24,  23.00,  21.00, @now - INTERVAL 1 HOUR);

-- ---------------------------------------------------------------------
-- Reservations
-- ---------------------------------------------------------------------
INSERT INTO reservations (id, buyer_id, product_id, quantity, unit_price, total_price, status, pickup_by, created_at) VALUES
 (1, 2, 6,  1, 200.00, 200.00, 'RESERVED',  @now + INTERVAL 30 MINUTE,  @now - INTERVAL 5 MINUTE),
 (2, 4, 24, 6,  23.00, 138.00, 'COMPLETED', @now - INTERVAL 80 MINUTE,  @now - INTERVAL 110 MINUTE),
 (3, 3, 24, 4,  21.00,  84.00, 'COMPLETED', @now - INTERVAL 20 MINUTE,  @now - INTERVAL 50 MINUTE);

-- ---------------------------------------------------------------------
-- Notifications
-- ---------------------------------------------------------------------
INSERT INTO notifications (user_id, type, message, product_id, is_read, created_at) VALUES
 (2,  'RESERVATION',  'Reservation confirmed: 1 x Cinnamon Rolls (pack of 6). Pay ₹200.00 at pickup.', 6, FALSE, @now - INTERVAL 5 MINUTE),
 (8,  'RESERVATION',  'Priya Sharma reserved 1 x Cinnamon Rolls (pack of 6).',                         6, FALSE, @now - INTERVAL 5 MINUTE),
 (2,  'PRICE_DROP',   'Fresh Bakery Cake (1 kg) dropped from ₹325.00 to ₹300.00.',                     1, FALSE, @now - INTERVAL 10 MINUTE),
 (4,  'ORDER_STATUS', 'Pickup completed: 6 x Spinach Bunch. Thank you for rescuing food!',             24, TRUE,  @now - INTERVAL 80 MINUTE),
 (8,  'AUCTION_EXPIRED', 'Your auction for Sourdough Loaf expired without a reservation.',             23, TRUE,  @now - INTERVAL 2 HOUR),
 (16, 'ACCOUNT',      'Your business Fresh Crumbs Bakery is waiting for admin approval.',            NULL, FALSE, @now - INTERVAL 1 HOUR),
 (1,  'ACCOUNT',      'New business registration: Fresh Crumbs Bakery is waiting for approval.',     NULL, FALSE, @now - INTERVAL 1 HOUR);
