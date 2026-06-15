ALTER TABLE users ADD COLUMN phone VARCHAR(50);
ALTER TABLE users ADD COLUMN notifications_email BOOLEAN DEFAULT TRUE;
ALTER TABLE users ADD COLUMN notifications_whatsapp BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN notifications_viber BOOLEAN DEFAULT FALSE;

CREATE TABLE broadcast (
   id           BIGSERIAL PRIMARY KEY,
   subject      VARCHAR(255),
   message      TEXT NOT NULL,
   channels     VARCHAR(100) NOT NULL,
   recipients   INTEGER DEFAULT 0,
   sent_at      TIMESTAMP NOT NULL DEFAULT NOW(),
   sent_by      VARCHAR(255)
);