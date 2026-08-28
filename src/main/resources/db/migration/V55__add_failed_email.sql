CREATE TABLE failed_email (
    id            BIGSERIAL PRIMARY KEY,
    recipient     VARCHAR(255) NOT NULL,
    subject       VARCHAR(500) NOT NULL,
    html_content  TEXT NOT NULL,
    attempts      INTEGER NOT NULL DEFAULT 0,
    last_error    VARCHAR(1000),
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    next_retry_at TIMESTAMP NOT NULL DEFAULT now(),
    sent_at       TIMESTAMP
);

-- keeps the scheduled "what's due for retry" query cheap as the table grows
CREATE INDEX idx_failed_email_retry ON failed_email (status, next_retry_at);
