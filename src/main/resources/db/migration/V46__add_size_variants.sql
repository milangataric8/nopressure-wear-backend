CREATE TABLE product_variant (
    id             BIGSERIAL PRIMARY KEY,
    product_id     BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    size           VARCHAR(10) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    sku            VARCHAR(100),
    CONSTRAINT uq_product_size UNIQUE (product_id, size)
);

CREATE INDEX idx_product_variant_product ON product_variant(product_id);

-- Backfill: split existing product stock evenly across S/M/L/XL (remainder goes to S)
INSERT INTO product_variant (product_id, size, stock_quantity)
SELECT p.id, s.size,
       CASE WHEN s.size = 'S'
            THEN (COALESCE(p.stock_quantity, 0) / 4) + (COALESCE(p.stock_quantity, 0) % 4)
            ELSE (COALESCE(p.stock_quantity, 0) / 4)
       END
FROM product p
CROSS JOIN (VALUES ('S'), ('M'), ('L'), ('XL')) AS s(size);

ALTER TABLE cart_item  ADD COLUMN size VARCHAR(10);
ALTER TABLE order_item ADD COLUMN size VARCHAR(10);
