CREATE TABLE banner (
                        id            BIGSERIAL PRIMARY KEY,
                        title         VARCHAR(255) NOT NULL,
                        subtitle      VARCHAR(255),
                        media_url     VARCHAR(500),
                        media_type    VARCHAR(10) NOT NULL DEFAULT 'IMAGE',
                        button_text   VARCHAR(100),
                        button_link   VARCHAR(255),
                        display_order INTEGER NOT NULL DEFAULT 0,
                        is_active     BOOLEAN NOT NULL DEFAULT TRUE
);