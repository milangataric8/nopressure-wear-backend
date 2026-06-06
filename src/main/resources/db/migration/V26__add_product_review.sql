CREATE TABLE product_review (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES product(id),
    user_id     BIGINT NOT NULL REFERENCES users(id),
    rating      INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(product_id, user_id)
);

ALTER TABLE product ADD COLUMN average_rating DECIMAL(2,1) DEFAULT 0;
ALTER TABLE product ADD COLUMN rating_count INTEGER DEFAULT 0;