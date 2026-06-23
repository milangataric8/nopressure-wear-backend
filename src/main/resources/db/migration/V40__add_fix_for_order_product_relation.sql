-- Snapshot product details onto order_item
ALTER TABLE order_item ADD COLUMN product_name VARCHAR(255);
ALTER TABLE order_item ADD COLUMN product_sku VARCHAR(100);
ALTER TABLE order_item ADD COLUMN product_image_url VARCHAR(500);

-- Backfill existing rows from the linked product
UPDATE order_item oi
SET product_name = p.name,
    product_sku = p.sku,
    product_image_url = p.image_url
    FROM product p
WHERE oi.product_id = p.id;

-- Make product_id nullable and drop the NOT NULL / keep FK but allow null on delete
ALTER TABLE order_item ALTER COLUMN product_id DROP NOT NULL;

-- Drop existing FK (name may differ — check your DB)
ALTER TABLE order_item DROP CONSTRAINT IF EXISTS fk_order_item_product;
ALTER TABLE order_item DROP CONSTRAINT IF EXISTS order_item_product_id_fkey;

-- Recreate with ON DELETE SET NULL
ALTER TABLE order_item
    ADD CONSTRAINT order_item_product_id_fkey
        FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE SET NULL;