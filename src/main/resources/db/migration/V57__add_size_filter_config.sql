-- Size filter. Unlike the other five filters, size lives on product_variant
-- (keyed by product_id + size), so the query resolves it with an EXISTS check.
-- 'multi-select' is a new filter_type: shoppers routinely want "M or L".
INSERT INTO filter_config (field_name, display_name, filter_type, is_visible, display_order)
VALUES ('size', 'Size', 'multi-select', true, 6);
