DROP TABLE broadcast;

CREATE TABLE notification (
   id           BIGSERIAL PRIMARY KEY,
   subject      VARCHAR(255),
   message      TEXT NOT NULL,
   image_url    VARCHAR(500),
   channels     VARCHAR(100) NOT NULL,
   recipients   INTEGER DEFAULT 0,
   sent_at      TIMESTAMP NOT NULL DEFAULT NOW(),
   sent_by      VARCHAR(255)
);