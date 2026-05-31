CREATE TABLE popup (
                       id            BIGSERIAL PRIMARY KEY,
                       title         VARCHAR(255) NOT NULL,
                       subtitle      VARCHAR(255),
                       content       TEXT,
                       media_url     VARCHAR(500),
                       media_type    VARCHAR(10) DEFAULT 'IMAGE',
                       button_text   VARCHAR(100),
                       button_link   VARCHAR(255),
                       background_color VARCHAR(7) DEFAULT '#FFFFFF',
                       text_color    VARCHAR(7) DEFAULT '#000000',
                       is_active     BOOLEAN NOT NULL DEFAULT TRUE,
                       show_once     BOOLEAN NOT NULL DEFAULT FALSE,
                       created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);