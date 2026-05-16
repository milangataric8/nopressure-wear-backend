DELETE FROM store_settings WHERE key IN ('footer_map_lat', 'footer_map_lng');

INSERT INTO store_settings (key, value, label) VALUES
    ('footer_map_address', 'Bulevar Oslobodjenja 15, Novi Sad, Serbia', 'Map Address');
