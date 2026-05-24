CREATE TABLE product_attribute (
    id           BIGSERIAL PRIMARY KEY,
    product_id   BIGINT NOT NULL REFERENCES product(id),
    attribute_key   VARCHAR(100) NOT NULL,
    attribute_value VARCHAR(255) NOT NULL
);

CREATE INDEX idx_product_attribute_key ON product_attribute(attribute_key);