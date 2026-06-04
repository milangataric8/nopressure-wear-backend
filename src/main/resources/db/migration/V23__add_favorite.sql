CREATE TABLE favorite (
                          id          BIGSERIAL PRIMARY KEY,
                          user_id     BIGINT NOT NULL REFERENCES users(id),
                          product_id  BIGINT NOT NULL REFERENCES product(id),
                          created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                          UNIQUE(user_id, product_id)
);