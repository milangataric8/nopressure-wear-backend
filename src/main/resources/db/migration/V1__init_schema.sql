CREATE TABLE category (
                          id          BIGSERIAL PRIMARY KEY,
                          name        VARCHAR(100) NOT NULL UNIQUE,
                          description VARCHAR(255),
                          parent_id   BIGINT REFERENCES category(id)
);

CREATE TABLE users (
                       id         BIGSERIAL PRIMARY KEY,
                       first_name VARCHAR(100) NOT NULL,
                       last_name  VARCHAR(100) NOT NULL,
                       email      VARCHAR(255) NOT NULL UNIQUE,
                       password   VARCHAR(255) NOT NULL,
                       role       VARCHAR(20)  NOT NULL,
                       is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP    NOT NULL,
                       updated_at TIMESTAMP    NOT NULL
);

CREATE TABLE address (
                         id          BIGSERIAL PRIMARY KEY,
                         user_id     BIGINT REFERENCES users(id),
                         street      VARCHAR(255) NOT NULL,
                         city        VARCHAR(100) NOT NULL,
                         postal_code VARCHAR(20)  NOT NULL,
                         country     VARCHAR(100) NOT NULL
);

CREATE TABLE product (
                         id             BIGSERIAL PRIMARY KEY,
                         name           VARCHAR(255)   NOT NULL,
                         description    TEXT,
                         price          NUMERIC(10, 2) NOT NULL,
                         stock_quantity INTEGER        NOT NULL,
                         sku            VARCHAR(100)   NOT NULL UNIQUE,
                         image_url      VARCHAR(500),
                         is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
                         category_id    BIGINT REFERENCES category(id),
                         created_at     TIMESTAMP      NOT NULL,
                         updated_at     TIMESTAMP      NOT NULL
);