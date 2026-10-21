-- Plain PostgreSQL SQL: run the entire file in pgAdmin, DBeaver or psql.
-- Requires application migrations through V63 and images in uploads/products.
-- Initial price: 2990.00; stock: 10 per size. Existing catalog data is preserved.
-- The specified admin account is created or updated to the requested credentials.
BEGIN;
SET LOCAL lock_timeout = '10s';
LOCK TABLE users, category, product, product_image, product_variant,
    product_color_variant IN SHARE ROW EXCLUSIVE MODE;

-- SUPER_ADMIN, password stored as BCrypt.
INSERT INTO users (first_name, last_name, email, password, role, is_active,
    created_at, updated_at, email_verified, failed_login_attempts,
    token_version, password_change_required, preferred_locale)
VALUES ('Milan', 'Gataric', 'milan.gataric93@gmail.com',
    '$2a$12$bsCteJn3P.SOAE9UaKz47ub/OFQ0HnbdHRKUwbTFkFgdicfnrzrwS',
    'SUPER_ADMIN', true, now(), now(), true, 0, 0, false, 'sr')
ON CONFLICT (email) DO UPDATE SET
    password = EXCLUDED.password, role = EXCLUDED.role, is_active = true,
    email_verified = true, failed_login_attempts = 0, lock_until = NULL,
    reset_token = NULL, reset_token_expiry = NULL,
    password_change_required = false, token_version = users.token_version + 1,
    updated_at = now()
WHERE users.password IS DISTINCT FROM EXCLUDED.password
   OR users.role IS DISTINCT FROM EXCLUDED.role
   OR NOT users.is_active OR NOT users.email_verified
   OR users.failed_login_attempts <> 0 OR users.lock_until IS NOT NULL
   OR users.reset_token IS NOT NULL OR users.reset_token_expiry IS NOT NULL
   OR users.password_change_required;

INSERT INTO category (name, description, is_active)
VALUES ('No Pressure T-Shirts', 'No Pressure majice: Moto, C Row, Signature i Circle.', true)
ON CONFLICT (name) DO NOTHING;

-- Moto / Black
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure Moto T-Shirt - Black',
    '<p><strong><em>Wear your mindset. We proudly present the Moto T-Shirt. 🌿✨</em></strong></p><p><em>More than just a classic white tee, this piece carries our core message right on the chest: No Pressure. Be relaxed. Live easy. Designed for those who appreciate clean typography and pure simplicity, it serves as a daily reminder to filter out the noise and stay grounded.</em></p><p><em>Crafted from soft, breathable cotton, it delivers maximum comfort whether you''re easing into your morning routine or stepping out for a relaxed evening.</em></p><p><em>🛒 Wear the mantra and embrace the calm.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-MOTO',
    '/uploads/products/nopressure_moto_tshirt_black.jpg', 'Black', '#1C1C1C', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-MOTO' AND color_name = 'Black');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Black'),
    '/uploads/products/nopressure_moto_tshirt_black.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Black') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Black')
      AND image_url = '/uploads/products/nopressure_moto_tshirt_black.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Black'), 'S', 10, 'NP-MOTO-BLACK-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Black'), 'M', 10, 'NP-MOTO-BLACK-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Black'), 'L', 10, 'NP-MOTO-BLACK-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Black'), 'XL', 10, 'NP-MOTO-BLACK-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- Moto / Green
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure Moto T-Shirt - Green',
    '<p><strong><em>Wear your mindset. We proudly present the Moto T-Shirt. 🌿✨</em></strong></p><p><em>More than just a classic white tee, this piece carries our core message right on the chest: No Pressure. Be relaxed. Live easy. Designed for those who appreciate clean typography and pure simplicity, it serves as a daily reminder to filter out the noise and stay grounded.</em></p><p><em>Crafted from soft, breathable cotton, it delivers maximum comfort whether you''re easing into your morning routine or stepping out for a relaxed evening.</em></p><p><em>🛒 Wear the mantra and embrace the calm.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-MOTO',
    '/uploads/products/nopressure_moto_tshirt_green.jpg', 'Green', '#526B5C', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-MOTO' AND color_name = 'Green');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Green'),
    '/uploads/products/nopressure_moto_tshirt_green.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Green') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Green')
      AND image_url = '/uploads/products/nopressure_moto_tshirt_green.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Green'), 'S', 10, 'NP-MOTO-GREEN-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Green'), 'M', 10, 'NP-MOTO-GREEN-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Green'), 'L', 10, 'NP-MOTO-GREEN-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Green'), 'XL', 10, 'NP-MOTO-GREEN-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- Moto / Ivory
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure Moto T-Shirt - Ivory',
    '<p><strong><em>Wear your mindset. We proudly present the Moto T-Shirt. 🌿✨</em></strong></p><p><em>More than just a classic white tee, this piece carries our core message right on the chest: No Pressure. Be relaxed. Live easy. Designed for those who appreciate clean typography and pure simplicity, it serves as a daily reminder to filter out the noise and stay grounded.</em></p><p><em>Crafted from soft, breathable cotton, it delivers maximum comfort whether you''re easing into your morning routine or stepping out for a relaxed evening.</em></p><p><em>🛒 Wear the mantra and embrace the calm.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-MOTO',
    '/uploads/products/nopressure_moto_tshirt_ivory.jpg', 'Ivory', '#F3EEDC', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-MOTO' AND color_name = 'Ivory');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Ivory'),
    '/uploads/products/nopressure_moto_tshirt_ivory.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Ivory') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Ivory')
      AND image_url = '/uploads/products/nopressure_moto_tshirt_ivory.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Ivory'), 'S', 10, 'NP-MOTO-IVORY-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Ivory'), 'M', 10, 'NP-MOTO-IVORY-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Ivory'), 'L', 10, 'NP-MOTO-IVORY-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Ivory'), 'XL', 10, 'NP-MOTO-IVORY-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- Moto / Navy Blue
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure Moto T-Shirt - Navy Blue',
    '<p><strong><em>Wear your mindset. We proudly present the Moto T-Shirt. 🌿✨</em></strong></p><p><em>More than just a classic white tee, this piece carries our core message right on the chest: No Pressure. Be relaxed. Live easy. Designed for those who appreciate clean typography and pure simplicity, it serves as a daily reminder to filter out the noise and stay grounded.</em></p><p><em>Crafted from soft, breathable cotton, it delivers maximum comfort whether you''re easing into your morning routine or stepping out for a relaxed evening.</em></p><p><em>🛒 Wear the mantra and embrace the calm.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-MOTO',
    '/uploads/products/nopressure_moto_tshirt_navyblue.jpg', 'Navy Blue', '#24243C', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-MOTO' AND color_name = 'Navy Blue');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Navy Blue'),
    '/uploads/products/nopressure_moto_tshirt_navyblue.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Navy Blue') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Navy Blue')
      AND image_url = '/uploads/products/nopressure_moto_tshirt_navyblue.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Navy Blue'), 'S', 10, 'NP-MOTO-NAVYBLUE-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Navy Blue'), 'M', 10, 'NP-MOTO-NAVYBLUE-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Navy Blue'), 'L', 10, 'NP-MOTO-NAVYBLUE-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'Navy Blue'), 'XL', 10, 'NP-MOTO-NAVYBLUE-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- Moto / White
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure Moto T-Shirt - White',
    '<p><strong><em>Wear your mindset. We proudly present the Moto T-Shirt. 🌿✨</em></strong></p><p><em>More than just a classic white tee, this piece carries our core message right on the chest: No Pressure. Be relaxed. Live easy. Designed for those who appreciate clean typography and pure simplicity, it serves as a daily reminder to filter out the noise and stay grounded.</em></p><p><em>Crafted from soft, breathable cotton, it delivers maximum comfort whether you''re easing into your morning routine or stepping out for a relaxed evening.</em></p><p><em>🛒 Wear the mantra and embrace the calm.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-MOTO',
    '/uploads/products/nopressure_moto_tshirt_white.jpg', 'White', '#FFFFFF', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-MOTO' AND color_name = 'White');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'White'),
    '/uploads/products/nopressure_moto_tshirt_white.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'White') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'White')
      AND image_url = '/uploads/products/nopressure_moto_tshirt_white.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'White'), 'S', 10, 'NP-MOTO-WHITE-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'White'), 'M', 10, 'NP-MOTO-WHITE-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'White'), 'L', 10, 'NP-MOTO-WHITE-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-MOTO' AND color_name = 'White'), 'XL', 10, 'NP-MOTO-WHITE-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- C Row / Black
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure C Row T-Shirt - Black',
    '<p><strong>Barely there, always grounded. Meet the NoPressure Tiny Center Tee. ☁️👁️</strong></p><p>Sometimes, the quietest voice is the most powerful. This tee is for those who live by the philosophy of subtle, unspoken ease. It features our stacked signature "no pressure" logo shrunk down and centered on the chest—a tiny whisper in a world that never stops shouting.</p><p>Made from ultra-soft, garment-dyed or vintage-wash natural cotton that feels broken-in from day one, its relaxed fit invites you to just <em>be</em>. It’s a minimalist''s dream, giving you the ultimate canvas for a stress-free day.</p><p>🛒 Keep it effortless, wear the whisper, and embrace the calm.</p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-ROW',
    '/uploads/products/nopressure_c_row_black.jpg', 'Black', '#1C1C1C', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-ROW' AND color_name = 'Black');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Black'),
    '/uploads/products/nopressure_c_row_black.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Black') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Black')
      AND image_url = '/uploads/products/nopressure_c_row_black.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Black'), 'S', 10, 'NP-ROW-BLACK-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Black'), 'M', 10, 'NP-ROW-BLACK-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Black'), 'L', 10, 'NP-ROW-BLACK-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Black'), 'XL', 10, 'NP-ROW-BLACK-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- C Row / Green
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure C Row T-Shirt - Green',
    '<p><strong>Barely there, always grounded. Meet the NoPressure Tiny Center Tee. ☁️👁️</strong></p><p>Sometimes, the quietest voice is the most powerful. This tee is for those who live by the philosophy of subtle, unspoken ease. It features our stacked signature "no pressure" logo shrunk down and centered on the chest—a tiny whisper in a world that never stops shouting.</p><p>Made from ultra-soft, garment-dyed or vintage-wash natural cotton that feels broken-in from day one, its relaxed fit invites you to just <em>be</em>. It’s a minimalist''s dream, giving you the ultimate canvas for a stress-free day.</p><p>🛒 Keep it effortless, wear the whisper, and embrace the calm.</p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-ROW',
    '/uploads/products/nopressure_c_row_green.jpg', 'Green', '#526B5C', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-ROW' AND color_name = 'Green');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Green'),
    '/uploads/products/nopressure_c_row_green.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Green') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Green')
      AND image_url = '/uploads/products/nopressure_c_row_green.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Green'), 'S', 10, 'NP-ROW-GREEN-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Green'), 'M', 10, 'NP-ROW-GREEN-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Green'), 'L', 10, 'NP-ROW-GREEN-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Green'), 'XL', 10, 'NP-ROW-GREEN-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- C Row / Ivory
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure C Row T-Shirt - Ivory',
    '<p><strong>Barely there, always grounded. Meet the NoPressure Tiny Center Tee. ☁️👁️</strong></p><p>Sometimes, the quietest voice is the most powerful. This tee is for those who live by the philosophy of subtle, unspoken ease. It features our stacked signature "no pressure" logo shrunk down and centered on the chest—a tiny whisper in a world that never stops shouting.</p><p>Made from ultra-soft, garment-dyed or vintage-wash natural cotton that feels broken-in from day one, its relaxed fit invites you to just <em>be</em>. It’s a minimalist''s dream, giving you the ultimate canvas for a stress-free day.</p><p>🛒 Keep it effortless, wear the whisper, and embrace the calm.</p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-ROW',
    '/uploads/products/nopressure_c_row_ivory.jpg', 'Ivory', '#F3EEDC', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-ROW' AND color_name = 'Ivory');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Ivory'),
    '/uploads/products/nopressure_c_row_ivory.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Ivory') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Ivory')
      AND image_url = '/uploads/products/nopressure_c_row_ivory.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Ivory'), 'S', 10, 'NP-ROW-IVORY-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Ivory'), 'M', 10, 'NP-ROW-IVORY-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Ivory'), 'L', 10, 'NP-ROW-IVORY-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Ivory'), 'XL', 10, 'NP-ROW-IVORY-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- C Row / Navy Blue
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure C Row T-Shirt - Navy Blue',
    '<p><strong>Barely there, always grounded. Meet the NoPressure Tiny Center Tee. ☁️👁️</strong></p><p>Sometimes, the quietest voice is the most powerful. This tee is for those who live by the philosophy of subtle, unspoken ease. It features our stacked signature "no pressure" logo shrunk down and centered on the chest—a tiny whisper in a world that never stops shouting.</p><p>Made from ultra-soft, garment-dyed or vintage-wash natural cotton that feels broken-in from day one, its relaxed fit invites you to just <em>be</em>. It’s a minimalist''s dream, giving you the ultimate canvas for a stress-free day.</p><p>🛒 Keep it effortless, wear the whisper, and embrace the calm.</p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-ROW',
    '/uploads/products/nopressure_c_row_navyblue.jpg', 'Navy Blue', '#24243C', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-ROW' AND color_name = 'Navy Blue');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Navy Blue'),
    '/uploads/products/nopressure_c_row_navyblue.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Navy Blue') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Navy Blue')
      AND image_url = '/uploads/products/nopressure_c_row_navyblue.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Navy Blue'), 'S', 10, 'NP-ROW-NAVYBLUE-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Navy Blue'), 'M', 10, 'NP-ROW-NAVYBLUE-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Navy Blue'), 'L', 10, 'NP-ROW-NAVYBLUE-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'Navy Blue'), 'XL', 10, 'NP-ROW-NAVYBLUE-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- C Row / White
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure C Row T-Shirt - White',
    '<p><strong>Barely there, always grounded. Meet the NoPressure Tiny Center Tee. ☁️👁️</strong></p><p>Sometimes, the quietest voice is the most powerful. This tee is for those who live by the philosophy of subtle, unspoken ease. It features our stacked signature "no pressure" logo shrunk down and centered on the chest—a tiny whisper in a world that never stops shouting.</p><p>Made from ultra-soft, garment-dyed or vintage-wash natural cotton that feels broken-in from day one, its relaxed fit invites you to just <em>be</em>. It’s a minimalist''s dream, giving you the ultimate canvas for a stress-free day.</p><p>🛒 Keep it effortless, wear the whisper, and embrace the calm.</p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-ROW',
    '/uploads/products/nopressure_c_row_white.jpg', 'White', '#FFFFFF', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-ROW' AND color_name = 'White');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'White'),
    '/uploads/products/nopressure_c_row_white.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'White') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'White')
      AND image_url = '/uploads/products/nopressure_c_row_white.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'White'), 'S', 10, 'NP-ROW-WHITE-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'White'), 'M', 10, 'NP-ROW-WHITE-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'White'), 'L', 10, 'NP-ROW-WHITE-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-ROW' AND color_name = 'White'), 'XL', 10, 'NP-ROW-WHITE-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- Signature / Black
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure Signature T-Shirt - Black',
    '<p><strong><em>Less pressure, more style. We proudly present our Signature model. 🌊✨</em></strong></p><p><em>The minimalist design, with a fluid line across the chest, is made for anyone who chooses peace over hustle and simplicity over complication.</em></p><p><em>Whether you choose the crisp white version or the powerful dark tone, the Signature T-shirt brings you that invaluable feeling of ease the moment you put it on.</em></p><p><em>🛒 Find your favorite fit and release the pressure.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-SIGNATURE',
    '/uploads/products/nopressure_signature_tshirt_black.jpg', 'Black', '#1C1C1C', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-SIGNATURE' AND color_name = 'Black');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Black'),
    '/uploads/products/nopressure_signature_tshirt_black.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Black') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Black')
      AND image_url = '/uploads/products/nopressure_signature_tshirt_black.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Black'), 'S', 10, 'NP-SIGNATURE-BLACK-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Black'), 'M', 10, 'NP-SIGNATURE-BLACK-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Black'), 'L', 10, 'NP-SIGNATURE-BLACK-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Black'), 'XL', 10, 'NP-SIGNATURE-BLACK-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- Signature / Green
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure Signature T-Shirt - Green',
    '<p><strong><em>Less pressure, more style. We proudly present our Signature model. 🌊✨</em></strong></p><p><em>The minimalist design, with a fluid line across the chest, is made for anyone who chooses peace over hustle and simplicity over complication.</em></p><p><em>Whether you choose the crisp white version or the powerful dark tone, the Signature T-shirt brings you that invaluable feeling of ease the moment you put it on.</em></p><p><em>🛒 Find your favorite fit and release the pressure.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-SIGNATURE',
    '/uploads/products/nopressure_signature_tshirt_green.jpg', 'Green', '#526B5C', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-SIGNATURE' AND color_name = 'Green');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Green'),
    '/uploads/products/nopressure_signature_tshirt_green.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Green') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Green')
      AND image_url = '/uploads/products/nopressure_signature_tshirt_green.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Green'), 'S', 10, 'NP-SIGNATURE-GREEN-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Green'), 'M', 10, 'NP-SIGNATURE-GREEN-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Green'), 'L', 10, 'NP-SIGNATURE-GREEN-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Green'), 'XL', 10, 'NP-SIGNATURE-GREEN-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- Signature / Ivory
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure Signature T-Shirt - Ivory',
    '<p><strong><em>Less pressure, more style. We proudly present our Signature model. 🌊✨</em></strong></p><p><em>The minimalist design, with a fluid line across the chest, is made for anyone who chooses peace over hustle and simplicity over complication.</em></p><p><em>Whether you choose the crisp white version or the powerful dark tone, the Signature T-shirt brings you that invaluable feeling of ease the moment you put it on.</em></p><p><em>🛒 Find your favorite fit and release the pressure.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-SIGNATURE',
    '/uploads/products/nopressure_signature_tshirt_ivory.jpg', 'Ivory', '#F3EEDC', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-SIGNATURE' AND color_name = 'Ivory');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Ivory'),
    '/uploads/products/nopressure_signature_tshirt_ivory.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Ivory') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Ivory')
      AND image_url = '/uploads/products/nopressure_signature_tshirt_ivory.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Ivory'), 'S', 10, 'NP-SIGNATURE-IVORY-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Ivory'), 'M', 10, 'NP-SIGNATURE-IVORY-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Ivory'), 'L', 10, 'NP-SIGNATURE-IVORY-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Ivory'), 'XL', 10, 'NP-SIGNATURE-IVORY-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- Signature / Navy Blue
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure Signature T-Shirt - Navy Blue',
    '<p><strong><em>Less pressure, more style. We proudly present our Signature model. 🌊✨</em></strong></p><p><em>The minimalist design, with a fluid line across the chest, is made for anyone who chooses peace over hustle and simplicity over complication.</em></p><p><em>Whether you choose the crisp white version or the powerful dark tone, the Signature T-shirt brings you that invaluable feeling of ease the moment you put it on.</em></p><p><em>🛒 Find your favorite fit and release the pressure.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-SIGNATURE',
    '/uploads/products/nopressure_signature_tshirt_navyblue.jpg', 'Navy Blue', '#24243C', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-SIGNATURE' AND color_name = 'Navy Blue');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Navy Blue'),
    '/uploads/products/nopressure_signature_tshirt_navyblue.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Navy Blue') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Navy Blue')
      AND image_url = '/uploads/products/nopressure_signature_tshirt_navyblue.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Navy Blue'), 'S', 10, 'NP-SIGNATURE-NAVYBLUE-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Navy Blue'), 'M', 10, 'NP-SIGNATURE-NAVYBLUE-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Navy Blue'), 'L', 10, 'NP-SIGNATURE-NAVYBLUE-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'Navy Blue'), 'XL', 10, 'NP-SIGNATURE-NAVYBLUE-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- Signature / White
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure Signature T-Shirt - White',
    '<p><strong><em>Less pressure, more style. We proudly present our Signature model. 🌊✨</em></strong></p><p><em>The minimalist design, with a fluid line across the chest, is made for anyone who chooses peace over hustle and simplicity over complication.</em></p><p><em>Whether you choose the crisp white version or the powerful dark tone, the Signature T-shirt brings you that invaluable feeling of ease the moment you put it on.</em></p><p><em>🛒 Find your favorite fit and release the pressure.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-SIGNATURE',
    '/uploads/products/nopressure_signature_tshirt_white.jpg', 'White', '#FFFFFF', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-SIGNATURE' AND color_name = 'White');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'White'),
    '/uploads/products/nopressure_signature_tshirt_white.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'White') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'White')
      AND image_url = '/uploads/products/nopressure_signature_tshirt_white.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'White'), 'S', 10, 'NP-SIGNATURE-WHITE-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'White'), 'M', 10, 'NP-SIGNATURE-WHITE-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'White'), 'L', 10, 'NP-SIGNATURE-WHITE-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-SIGNATURE' AND color_name = 'White'), 'XL', 10, 'NP-SIGNATURE-WHITE-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- Circle / Black
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure Circle T-Shirt - Black',
    '<p><strong><em>A perfect balance of form and focus. Meet the NoPressure Circle Tee. ⭕✨</em></strong></p><p><em>Geometric simplicity meets everyday ease. The </em><strong><em>Circle Tee</em></strong><em> features a fine, clean circular outline centered on the chest, framing our signature name with a subtle pop of red on "no". The circle represents completeness, balance, and creating your own peaceful bubble amidst the outer noise.</em></p><p><em>Crafted from soft, premium cotton with a relaxed fit, it offers maximum comfort for your everyday moves. It''s a clean, graphic statement designed for those who appreciate modern minimalism with a thoughtful edge.</em></p><p><em>🛒 Find your balance and step out in effortless style.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-CIRCLE',
    '/uploads/products/nopressure_circle_tshirt_black.jpg', 'Black', '#1C1C1C', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-CIRCLE' AND color_name = 'Black');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Black'),
    '/uploads/products/nopressure_circle_tshirt_black.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Black') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Black')
      AND image_url = '/uploads/products/nopressure_circle_tshirt_black.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Black'), 'S', 10, 'NP-CIRCLE-BLACK-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Black'), 'M', 10, 'NP-CIRCLE-BLACK-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Black'), 'L', 10, 'NP-CIRCLE-BLACK-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Black'), 'XL', 10, 'NP-CIRCLE-BLACK-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- Circle / Green
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure Circle T-Shirt - Green',
    '<p><strong><em>A perfect balance of form and focus. Meet the NoPressure Circle Tee. ⭕✨</em></strong></p><p><em>Geometric simplicity meets everyday ease. The </em><strong><em>Circle Tee</em></strong><em> features a fine, clean circular outline centered on the chest, framing our signature name with a subtle pop of red on "no". The circle represents completeness, balance, and creating your own peaceful bubble amidst the outer noise.</em></p><p><em>Crafted from soft, premium cotton with a relaxed fit, it offers maximum comfort for your everyday moves. It''s a clean, graphic statement designed for those who appreciate modern minimalism with a thoughtful edge.</em></p><p><em>🛒 Find your balance and step out in effortless style.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-CIRCLE',
    '/uploads/products/nopressure_circle_tshirt_green.jpg', 'Green', '#526B5C', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-CIRCLE' AND color_name = 'Green');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Green'),
    '/uploads/products/nopressure_circle_tshirt_green.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Green') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Green')
      AND image_url = '/uploads/products/nopressure_circle_tshirt_green.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Green'), 'S', 10, 'NP-CIRCLE-GREEN-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Green'), 'M', 10, 'NP-CIRCLE-GREEN-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Green'), 'L', 10, 'NP-CIRCLE-GREEN-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Green'), 'XL', 10, 'NP-CIRCLE-GREEN-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- Circle / Ivory
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure Circle T-Shirt - Ivory',
    '<p><strong><em>A perfect balance of form and focus. Meet the NoPressure Circle Tee. ⭕✨</em></strong></p><p><em>Geometric simplicity meets everyday ease. The </em><strong><em>Circle Tee</em></strong><em> features a fine, clean circular outline centered on the chest, framing our signature name with a subtle pop of red on "no". The circle represents completeness, balance, and creating your own peaceful bubble amidst the outer noise.</em></p><p><em>Crafted from soft, premium cotton with a relaxed fit, it offers maximum comfort for your everyday moves. It''s a clean, graphic statement designed for those who appreciate modern minimalism with a thoughtful edge.</em></p><p><em>🛒 Find your balance and step out in effortless style.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-CIRCLE',
    '/uploads/products/nopressure_circle_tshirt_ivory.jpg', 'Ivory', '#F3EEDC', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-CIRCLE' AND color_name = 'Ivory');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Ivory'),
    '/uploads/products/nopressure_circle_tshirt_ivory.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Ivory') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Ivory')
      AND image_url = '/uploads/products/nopressure_circle_tshirt_ivory.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Ivory'), 'S', 10, 'NP-CIRCLE-IVORY-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Ivory'), 'M', 10, 'NP-CIRCLE-IVORY-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Ivory'), 'L', 10, 'NP-CIRCLE-IVORY-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Ivory'), 'XL', 10, 'NP-CIRCLE-IVORY-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- Circle / Navy Blue
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure Circle T-Shirt - Navy Blue',
    '<p><strong><em>A perfect balance of form and focus. Meet the NoPressure Circle Tee. ⭕✨</em></strong></p><p><em>Geometric simplicity meets everyday ease. The </em><strong><em>Circle Tee</em></strong><em> features a fine, clean circular outline centered on the chest, framing our signature name with a subtle pop of red on "no". The circle represents completeness, balance, and creating your own peaceful bubble amidst the outer noise.</em></p><p><em>Crafted from soft, premium cotton with a relaxed fit, it offers maximum comfort for your everyday moves. It''s a clean, graphic statement designed for those who appreciate modern minimalism with a thoughtful edge.</em></p><p><em>🛒 Find your balance and step out in effortless style.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-CIRCLE',
    '/uploads/products/nopressure_circle_tshirt_navyblue.jpg', 'Navy Blue', '#24243C', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-CIRCLE' AND color_name = 'Navy Blue');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Navy Blue'),
    '/uploads/products/nopressure_circle_tshirt_navyblue.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Navy Blue') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Navy Blue')
      AND image_url = '/uploads/products/nopressure_circle_tshirt_navyblue.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Navy Blue'), 'S', 10, 'NP-CIRCLE-NAVYBLUE-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Navy Blue'), 'M', 10, 'NP-CIRCLE-NAVYBLUE-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Navy Blue'), 'L', 10, 'NP-CIRCLE-NAVYBLUE-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'Navy Blue'), 'XL', 10, 'NP-CIRCLE-NAVYBLUE-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- Circle / White
INSERT INTO product (name, description, price, sku, image_url, color_name,
    color_hex, is_active, category_id, created_at, updated_at, brand,
    average_rating, rating_count, discount_percentage, sales_count, gender)
SELECT 'No Pressure Circle T-Shirt - White',
    '<p><strong><em>A perfect balance of form and focus. Meet the NoPressure Circle Tee. ⭕✨</em></strong></p><p><em>Geometric simplicity meets everyday ease. The </em><strong><em>Circle Tee</em></strong><em> features a fine, clean circular outline centered on the chest, framing our signature name with a subtle pop of red on "no". The circle represents completeness, balance, and creating your own peaceful bubble amidst the outer noise.</em></p><p><em>Crafted from soft, premium cotton with a relaxed fit, it offers maximum comfort for your everyday moves. It''s a clean, graphic statement designed for those who appreciate modern minimalism with a thoughtful edge.</em></p><p><em>🛒 Find your balance and step out in effortless style.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>', 2990.00, 'NP-CIRCLE',
    '/uploads/products/nopressure_circle_tshirt_white.jpg', 'White', '#FFFFFF', true,
    (SELECT id FROM category WHERE name = 'No Pressure T-Shirts'),
    now(), now(), 'No Pressure', 0, 0, 0, 0, 'UNISEX'
WHERE NOT EXISTS (SELECT 1 FROM product
    WHERE sku = 'NP-CIRCLE' AND color_name = 'White');

INSERT INTO product_image (product_id, image_url, display_order, is_primary)
SELECT (SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'White'),
    '/uploads/products/nopressure_circle_tshirt_white.jpg', 0,
    NOT EXISTS (SELECT 1 FROM product_image
        WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'White') AND is_primary)
WHERE NOT EXISTS (SELECT 1 FROM product_image
    WHERE product_id = (SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'White')
      AND image_url = '/uploads/products/nopressure_circle_tshirt_white.jpg');

INSERT INTO product_variant (product_id, size, stock_quantity, sku, low_stock_alerted)
VALUES
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'White'), 'S', 10, 'NP-CIRCLE-WHITE-S', false),
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'White'), 'M', 10, 'NP-CIRCLE-WHITE-M', false),
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'White'), 'L', 10, 'NP-CIRCLE-WHITE-L', false),
    ((SELECT id FROM product WHERE sku = 'NP-CIRCLE' AND color_name = 'White'), 'XL', 10, 'NP-CIRCLE-WHITE-XL', false)
ON CONFLICT (product_id, size) DO NOTHING;

-- Each model: link every color to the other four, in both directions.

INSERT INTO product_color_variant (product_id, variant_id)
SELECT p.id, v.id
FROM product p JOIN product v ON v.sku = p.sku AND v.id <> p.id
WHERE p.sku = 'NP-MOTO'
  AND p.color_name IN ('Black', 'Green', 'Ivory', 'Navy Blue', 'White')
  AND v.color_name IN ('Black', 'Green', 'Ivory', 'Navy Blue', 'White')
  AND NOT EXISTS (SELECT 1 FROM product_color_variant cv
      WHERE cv.product_id = p.id AND cv.variant_id = v.id);

INSERT INTO product_color_variant (product_id, variant_id)
SELECT p.id, v.id
FROM product p JOIN product v ON v.sku = p.sku AND v.id <> p.id
WHERE p.sku = 'NP-ROW'
  AND p.color_name IN ('Black', 'Green', 'Ivory', 'Navy Blue', 'White')
  AND v.color_name IN ('Black', 'Green', 'Ivory', 'Navy Blue', 'White')
  AND NOT EXISTS (SELECT 1 FROM product_color_variant cv
      WHERE cv.product_id = p.id AND cv.variant_id = v.id);

INSERT INTO product_color_variant (product_id, variant_id)
SELECT p.id, v.id
FROM product p JOIN product v ON v.sku = p.sku AND v.id <> p.id
WHERE p.sku = 'NP-SIGNATURE'
  AND p.color_name IN ('Black', 'Green', 'Ivory', 'Navy Blue', 'White')
  AND v.color_name IN ('Black', 'Green', 'Ivory', 'Navy Blue', 'White')
  AND NOT EXISTS (SELECT 1 FROM product_color_variant cv
      WHERE cv.product_id = p.id AND cv.variant_id = v.id);

INSERT INTO product_color_variant (product_id, variant_id)
SELECT p.id, v.id
FROM product p JOIN product v ON v.sku = p.sku AND v.id <> p.id
WHERE p.sku = 'NP-CIRCLE'
  AND p.color_name IN ('Black', 'Green', 'Ivory', 'Navy Blue', 'White')
  AND v.color_name IN ('Black', 'Green', 'Ivory', 'Navy Blue', 'White')
  AND NOT EXISTS (SELECT 1 FROM product_color_variant cv
      WHERE cv.product_id = p.id AND cv.variant_id = v.id);

-- BEGIN WEBSITE CONTENT (2026-10-21)
-- https://nopressurewear.com/products/no-pressure-moto-t-shirt-1
UPDATE product
SET description = $np$<p><strong><em>Wear your mindset. We proudly present the Moto T-Shirt. 🌿✨</em></strong></p><p><em>More than just a classic white tee, this piece carries our core message right on the chest: No Pressure. Be relaxed. Live easy. Designed for those who appreciate clean typography and pure simplicity, it serves as a daily reminder to filter out the noise and stay grounded.</em></p><p><em>Crafted from soft, breathable cotton, it delivers maximum comfort whether you're easing into your morning routine or stepping out for a relaxed evening.</em></p><p><em>🛒 Wear the mantra and embrace the calm.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>$np$, updated_at = now()
WHERE sku = 'NP-MOTO'
  AND color_name IN ('Black', 'Green', 'Ivory', 'Navy Blue', 'White');

-- https://nopressurewear.com/products/nopressure-c-row-t-shirt-1
UPDATE product
SET description = $np$<p><strong>Barely there, always grounded. Meet the NoPressure Tiny Center Tee. ☁️👁️</strong></p><p>Sometimes, the quietest voice is the most powerful. This tee is for those who live by the philosophy of subtle, unspoken ease. It features our stacked signature "no pressure" logo shrunk down and centered on the chest—a tiny whisper in a world that never stops shouting.</p><p>Made from ultra-soft, garment-dyed or vintage-wash natural cotton that feels broken-in from day one, its relaxed fit invites you to just <em>be</em>. It’s a minimalist's dream, giving you the ultimate canvas for a stress-free day.</p><p>🛒 Keep it effortless, wear the whisper, and embrace the calm.</p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>$np$, updated_at = now()
WHERE sku = 'NP-ROW'
  AND color_name IN ('Black', 'Green', 'Ivory', 'Navy Blue', 'White');

-- https://nopressurewear.com/products/minimal-script-serendipity-t-shirt
UPDATE product
SET description = $np$<p><strong><em>Less pressure, more style. We proudly present our Signature model. 🌊✨</em></strong></p><p><em>The minimalist design, with a fluid line across the chest, is made for anyone who chooses peace over hustle and simplicity over complication.</em></p><p><em>Whether you choose the crisp white version or the powerful dark tone, the Signature T-shirt brings you that invaluable feeling of ease the moment you put it on.</em></p><p><em>🛒 Find your favorite fit and release the pressure.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>$np$, updated_at = now()
WHERE sku = 'NP-SIGNATURE'
  AND color_name IN ('Black', 'Green', 'Ivory', 'Navy Blue', 'White');

-- https://nopressurewear.com/products/nopressure-circle-tee-1
UPDATE product
SET description = $np$<p><strong><em>A perfect balance of form and focus. Meet the NoPressure Circle Tee. ⭕✨</em></strong></p><p><em>Geometric simplicity meets everyday ease. The </em><strong><em>Circle Tee</em></strong><em> features a fine, clean circular outline centered on the chest, framing our signature name with a subtle pop of red on "no". The circle represents completeness, balance, and creating your own peaceful bubble amidst the outer noise.</em></p><p><em>Crafted from soft, premium cotton with a relaxed fit, it offers maximum comfort for your everyday moves. It's a clean, graphic statement designed for those who appreciate modern minimalism with a thoughtful edge.</em></p><p><em>🛒 Find your balance and step out in effortless style.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>$np$, updated_at = now()
WHERE sku = 'NP-CIRCLE'
  AND color_name IN ('Black', 'Green', 'Ivory', 'Navy Blue', 'White');

-- https://nopressurewear.com/ (brand text in footer)
UPDATE store_settings SET value = $np$<p><em><strong>Life's already complicated enough – your style doesn’t have to be.</strong></em></p><p><em><strong>NoPressure</strong></em><em> was created for those who choose to find their own rhythm in a world that’s always rushing.<br/>We believe that true strength comes from inner peace and the freedom to be yourself, free from societal expectations and imposed stress. Our pieces are designed for ultimate comfort, freedom of movement, and that invaluable feeling of ease.<br/>Slow down, take a breath, and embrace the moment. </em></p><p><em><strong>Be relaxed. Live Easy.</strong></em></p>$np$
WHERE key = 'store_tagline';
INSERT INTO store_settings (key, value, label)
SELECT 'store_tagline', $np$<p><em><strong>Life's already complicated enough – your style doesn’t have to be.</strong></em></p><p><em><strong>NoPressure</strong></em><em> was created for those who choose to find their own rhythm in a world that’s always rushing.<br/>We believe that true strength comes from inner peace and the freedom to be yourself, free from societal expectations and imposed stress. Our pieces are designed for ultimate comfort, freedom of movement, and that invaluable feeling of ease.<br/>Slow down, take a breath, and embrace the moment. </em></p><p><em><strong>Be relaxed. Live Easy.</strong></em></p>$np$, 'Tagline'
WHERE NOT EXISTS (SELECT 1 FROM store_settings WHERE key = 'store_tagline');
-- END WEBSITE CONTENT
COMMIT;