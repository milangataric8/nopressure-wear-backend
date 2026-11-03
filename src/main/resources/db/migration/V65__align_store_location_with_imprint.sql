-- Use the business address published in the imprint for contact, footer and map.
INSERT INTO store_settings (key, value, label) VALUES
    ('footer_address', 'Dragoslava Srejovica', 'Address'),
    ('footer_city', 'Vjeternik', 'City'),
    ('footer_map_address', 'Dragoslava Srejovica, Vjeternik', 'Map Address')
ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;
