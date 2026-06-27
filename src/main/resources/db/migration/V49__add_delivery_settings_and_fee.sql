INSERT INTO store_settings (key, value, label) VALUES
    ('delivery_enabled', 'true', 'Delivery Enabled'),
    ('delivery_fee', '400', 'Delivery Fee'),
    ('free_shipping_threshold', '5000', 'Free Shipping Threshold');

ALTER TABLE orders ADD COLUMN delivery_fee NUMERIC(10,2) NOT NULL DEFAULT 0;
