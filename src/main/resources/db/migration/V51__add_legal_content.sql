CREATE TABLE legal_content (
    id       BIGSERIAL PRIMARY KEY,
    type     VARCHAR(20)  NOT NULL,
    language VARCHAR(5)   NOT NULL,
    content  TEXT         NOT NULL DEFAULT '',
    last_updated TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_legal_type_lang UNIQUE (type, language)
);

INSERT INTO legal_content (type, language, content) VALUES
    ('PRIVACY',  'en', ''),
    ('PRIVACY',  'sr', ''),
    ('TERMS',    'en', ''),
    ('TERMS',    'sr', ''),
    ('RETURNS',  'en', ''),
    ('RETURNS',  'sr', ''),
    ('IMPRINT',  'en', ''),
    ('IMPRINT',  'sr', '');
