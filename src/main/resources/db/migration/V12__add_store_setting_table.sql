CREATE TABLE store_settings (
                               id       BIGSERIAL PRIMARY KEY,
                               key      VARCHAR(100) NOT NULL UNIQUE,
                               value    TEXT,
                               label    VARCHAR(255)
);

INSERT INTO store_settings (key, value, label) VALUES
('store_name', 'WEBSHOP', 'Store Name'),
('store_tagline', 'Your premium destination for quality products. We deliver the best shopping experience.', 'Tagline'),
('store_logo_url', '', 'Logo Image URL'),
('footer_address', 'Bulevar Oslobodjenja 15', 'Address'),
('footer_city', '21000 Novi Sad, Serbia', 'City'),
('footer_hours_weekday', 'Mon — Fri: 9:00 — 20:00', 'Weekday Hours'),
('footer_hours_saturday', 'Sat: 9:00 — 16:00', 'Saturday Hours'),
('footer_hours_sunday', 'Sun: Closed', 'Sunday Hours'),
('footer_email', 'info@webshop.com', 'Email'),
('footer_phone', '+381 21 123 456', 'Phone'),
('footer_map_lat', '45.2396', 'Map Latitude'),
('footer_map_lng', '19.8227', 'Map Longitude');