CREATE TABLE product_image (
    id            BIGSERIAL PRIMARY KEY,
    product_id    BIGINT NOT NULL REFERENCES product(id),
    image_url     VARCHAR(500) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_primary    BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE product_color (
    id                 BIGSERIAL PRIMARY KEY,
    product_id         BIGINT NOT NULL REFERENCES product(id),
    color_name         VARCHAR(100) NOT NULL,
    color_hex          VARCHAR(7) NOT NULL,
    variant_product_id BIGINT NOT NULL REFERENCES product(id)
);

ALTER TABLE product ADD COLUMN video_url VARCHAR(500);