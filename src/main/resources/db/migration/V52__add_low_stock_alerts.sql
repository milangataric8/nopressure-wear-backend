INSERT INTO store_settings (key, value, label) VALUES
    ('low_stock_alerts_enabled', 'true',  'Low-Stock Alerts'),
    ('low_stock_threshold',      '5',     'Low-Stock Threshold');

ALTER TABLE product_variant
    ADD COLUMN low_stock_alerted BOOLEAN NOT NULL DEFAULT FALSE;
