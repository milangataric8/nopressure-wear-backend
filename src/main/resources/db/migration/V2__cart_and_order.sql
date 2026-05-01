CREATE TABLE cart (
                      id      BIGSERIAL PRIMARY KEY,
                      user_id BIGINT NOT NULL UNIQUE REFERENCES users(id)
);

CREATE TABLE cart_item (
                           id         BIGSERIAL PRIMARY KEY,
                           cart_id    BIGINT         NOT NULL REFERENCES cart(id),
                           product_id BIGINT         NOT NULL REFERENCES product(id),
                           quantity   INTEGER        NOT NULL
);

CREATE TABLE orders (
                        id           BIGSERIAL PRIMARY KEY,
                        user_id      BIGINT         NOT NULL REFERENCES users(id),
                        status       VARCHAR(20)    NOT NULL,
                        total_amount NUMERIC(10, 2) NOT NULL,
                        created_at   TIMESTAMP      NOT NULL,
                        updated_at   TIMESTAMP      NOT NULL
);

CREATE TABLE order_item (
                            id                  BIGSERIAL PRIMARY KEY,
                            order_id            BIGINT         NOT NULL REFERENCES orders(id),
                            product_id          BIGINT         NOT NULL REFERENCES product(id),
                            quantity            INTEGER        NOT NULL,
                            price_at_purchase   NUMERIC(10, 2) NOT NULL
);