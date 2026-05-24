CREATE TABLE filter_config (
   id            BIGSERIAL PRIMARY KEY,
   field_name    VARCHAR(100) NOT NULL UNIQUE,
   display_name  VARCHAR(100) NOT NULL,
   filter_type   VARCHAR(20) NOT NULL,
   is_visible    BOOLEAN NOT NULL DEFAULT TRUE,
   display_order INTEGER NOT NULL DEFAULT 0
);

INSERT INTO filter_config (field_name, display_name, filter_type, is_visible, display_order) VALUES
    ('category', 'Categories', 'select', true, 1),
    ('colorName', 'Color', 'color', true, 2),
    ('brand', 'Brand', 'select', true, 3),
    ('price', 'Price', 'range', true, 4);