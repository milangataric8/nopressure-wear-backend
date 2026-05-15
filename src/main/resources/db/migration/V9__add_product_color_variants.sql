DROP TABLE product_color;

CREATE TABLE product_color_variant (
                                       id              BIGSERIAL PRIMARY KEY,
                                       product_id      BIGINT NOT NULL REFERENCES product(id),
                                       variant_id      BIGINT NOT NULL REFERENCES product(id)
);

ALTER TABLE product ADD COLUMN color_name VARCHAR(100);
ALTER TABLE product ADD COLUMN color_hex VARCHAR(7);