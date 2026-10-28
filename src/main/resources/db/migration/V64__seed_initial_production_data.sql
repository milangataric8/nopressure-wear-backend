-- V64__seed_initial_production_data.sql
-- Initial production catalog seed for NoPressure Wear.
-- Intentionally excludes user/admin creation and unrelated schema changes.

-- category
INSERT INTO category (id, name, description, parent_id, is_active) VALUES
    (1, 'Polo',    '<p></p>', NULL, true),
    (2, 'Regular', '<p></p>', NULL, true);

-- banner
INSERT INTO banner (
    id, title, subtitle, media_url, media_type, button_text, button_link,
    display_order, is_active, display_title, display_duration,
    mobile_media_url, mobile_media_type
) VALUES
    (1, '', '',
     '/uploads/products/6de48502-9055-4788-a864-01ec42dec077_nopressure_moto_shop_easy_cover.png',
     'IMAGE', '', '',
     0, true, false, 5,
     '/uploads/products/ca13a261-f464-47b0-a399-4f73424872f6_nopressure_wear_mobile_banner.png',
     'IMAGE'),
    (2, '', '',
     '/uploads/products/23cbccaa-2a0d-49ef-bf07-1184b0b3492d_nopressure_banner.png',
     'IMAGE', '', '',
     0, true, false, 5,
     '/uploads/products/43bb6e13-3cf1-4a6a-8d03-38d75a967aa9_nopressure_banner_mobile.jpg',
     'IMAGE');

-- product
INSERT INTO product (
    id, name, description, price, sku, image_url, is_active,
    category_id, created_at, updated_at, video_url, color_name, color_hex, brand,
    average_rating, rating_count, discount_percentage, discount_price,
    material, sales_count, gender
) VALUES
    (1, 'Signature', '', 2800.00, 'SIG-M-W',
     '/uploads/products/4305df4d-5f21-4897-9436-ac1404fa6850_nopressure_signature_shirt_w.png',
     true, 2, '2026-08-30 15:00:39.901201', '2026-08-30 15:02:17.830342',
     '', 'White', '#FFFFFF', '', NULL, NULL, 0.00, NULL, 'Pamuk', NULL, 'MEN'),
    (2, 'Signature', '', 2800.00, 'SIG-M-B',
     '/uploads/products/ffd94471-67c8-4adf-81c7-2a6472d3890e_nopressure_signature_shirt_b.png',
     true, 2, '2026-08-30 15:01:23.210555', '2026-08-30 15:02:26.304008',
     '', 'Black', '#000000', '', NULL, NULL, 0.00, NULL, 'Pamuk', NULL, 'MEN'),
    (3, 'Row Polo', '', 3600.00, 'ROW-POLO-M-W',
     '/uploads/products/5cea2d5b-6384-401b-b8e8-45767b78cd4d3890e_nopressure_row_polo_w.png',
     true, 1, '2026-08-30 15:04:00.174117', '2026-08-30 15:04:11.868711',
     '', 'White', '#FFFFFF', '', NULL, NULL, 0.00, NULL, 'Pamuk', NULL, 'MEN'),
    (4, 'Row Polo', '', 3600.00, 'ROW-POLO-M-B',
     '/uploads/products/8916e353-83de-449a-b15f-cb7a4509b40c_nopressure_row_polo_b.png',
     true, 1, '2026-08-30 15:04:53.116', '2026-08-30 15:12:30.312513',
     '', 'Black', '#000000', '', NULL, NULL, 0.00, NULL, 'Pamuk', NULL, 'MEN'),
    (5, 'Signature New w', '', 2800.00, 'SIG-M-W-NEW',
     '/uploads/products/4305df4d-5f21-4897-9436-ac1404fa6850_nopressure_signature_shirt_w.png',
     true, 2, '2026-08-30 15:00:39.901201', '2026-08-30 15:02:17.830342',
     '', 'White', '#FFFFFF', '', NULL, NULL, 0.00, NULL, 'Pamuk', NULL, 'MEN'),
    (6, 'Signature New', '', 2800.00, 'SIG-M-B-NEW',
     '/uploads/products/ffd94471-67c8-4adf-81c7-2a6472d3890e_nopressure_signature_shirt_b.png',
     true, 2, '2026-08-30 15:01:23.210555', '2026-08-30 15:02:26.304008',
     '', 'Black', '#000000', '', NULL, NULL, 0.00, NULL, 'Pamuk', NULL, 'MEN');

-- product_color_variant
-- Both product_id and variant_id reference product(id).
INSERT INTO product_color_variant (id, product_id, variant_id) VALUES
    (1, 2, 1),
    (2, 1, 2),
    (3, 4, 3),
    (4, 3, 4);

-- product_variant
INSERT INTO product_variant (id, product_id, size, stock_quantity, sku, low_stock_alerted) VALUES
    (29, 4, 'S',   5, NULL, false),
    (30, 4, 'M',  10, NULL, false),
    (31, 4, 'L',  10, NULL, false),
    (32, 4, 'XL',  5, NULL, false),
    (33, 3, 'S',   5, NULL, false),
    (34, 3, 'M',  10, NULL, false),
    (35, 3, 'L',  10, NULL, false),
    (36, 3, 'XL',  5, NULL, false),
    (37, 1, 'S',   5, NULL, false),
    (38, 1, 'M',  10, NULL, false),
    (39, 1, 'L',  10, NULL, false),
    (40, 1, 'XL',  5, NULL, false),
    (41, 2, 'S',   5, NULL, false),
    (42, 2, 'M',  10, NULL, false),
    (43, 2, 'L',  10, NULL, false),
    (44, 2, 'XL',  5, NULL, false);

-- Keep generated ID sequences aligned with the explicit seed IDs.
SELECT setval(pg_get_serial_sequence('category', 'id'), GREATEST((SELECT MAX(id) FROM category), 1), true);
SELECT setval(pg_get_serial_sequence('banner', 'id'), GREATEST((SELECT MAX(id) FROM banner), 1), true);
SELECT setval(pg_get_serial_sequence('product', 'id'), GREATEST((SELECT MAX(id) FROM product), 1), true);
SELECT setval(pg_get_serial_sequence('product_color_variant', 'id'), GREATEST((SELECT MAX(id) FROM product_color_variant), 1), true);
SELECT setval(pg_get_serial_sequence('product_variant', 'id'), GREATEST((SELECT MAX(id) FROM product_variant), 1), true);
