-- Stock is tracked per (product_id, size) on product_variant. product.stock_quantity
-- was an unmaintained aggregate duplicate and is no longer read anywhere.
ALTER TABLE product DROP COLUMN stock_quantity;
