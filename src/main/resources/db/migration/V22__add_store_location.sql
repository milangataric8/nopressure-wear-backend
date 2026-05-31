CREATE TABLE store_location (
                                id          BIGSERIAL PRIMARY KEY,
                                name        VARCHAR(255) NOT NULL,
                                street      VARCHAR(255) NOT NULL,
                                city        VARCHAR(100) NOT NULL,
                                postal_code VARCHAR(20),
                                country     VARCHAR(100) NOT NULL,
                                phone       VARCHAR(50),
                                email       VARCHAR(255),
                                working_hours VARCHAR(255),
                                is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE product_store (
                               id                BIGSERIAL PRIMARY KEY,
                               product_id        BIGINT NOT NULL REFERENCES product(id),
                               store_location_id BIGINT NOT NULL REFERENCES store_location(id),
                               in_stock          BOOLEAN NOT NULL DEFAULT TRUE,
                               UNIQUE(product_id, store_location_id)
);

INSERT INTO store_settings (key, value, label) VALUES
    ('find_in_store_enabled', 'true', 'Find in Store');