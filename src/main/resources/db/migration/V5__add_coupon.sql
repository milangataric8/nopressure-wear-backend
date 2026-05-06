CREATE TABLE coupon (
                        id             BIGSERIAL PRIMARY KEY,
                        code           VARCHAR(50)    NOT NULL UNIQUE,
                        discount_type  VARCHAR(20)    NOT NULL,
                        discount_value NUMERIC(10, 2) NOT NULL,
                        is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
                        expires_at     TIMESTAMP,
                        usage_limit    INTEGER        NOT NULL,
                        usage_count    INTEGER        NOT NULL DEFAULT 0,
                        created_at     TIMESTAMP      NOT NULL
);

ALTER TABLE orders ADD COLUMN coupon_code VARCHAR(50);
ALTER TABLE orders ADD COLUMN discount_amount NUMERIC(10, 2) DEFAULT 0;