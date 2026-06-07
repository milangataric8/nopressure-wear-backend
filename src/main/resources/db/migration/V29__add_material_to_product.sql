ALTER TABLE product ADD COLUMN material VARCHAR(100);

INSERT INTO filter_config (field_name, display_name, filter_type, is_visible, display_order)
VALUES ('material', 'Material', 'select', true, 5);