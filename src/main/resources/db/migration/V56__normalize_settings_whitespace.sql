-- Phase A whitespace normalization for rich-text rows already in the database.
--   * &nbsp; and literal U+00A0  -> ordinary space
--   * zero-width chars (U+200B / U+200C / U+200D / U+FEFF) and soft hyphen (U+00AD) -> removed
--   * runs of 2+ spaces/tabs -> single space
--   * leading/trailing whitespace trimmed
--
-- Phase B (deliberate &nbsp; re-insertion around units, abbreviations, initials and
-- one-letter prepositions) is intentionally NOT done here: it is typography polish that
-- lives in one place (HtmlTextSanitizer) and is re-applied the next time a record is
-- saved through the admin panel.

UPDATE store_settings
SET value = btrim(
    regexp_replace(
        regexp_replace(
            replace(replace(value, '&nbsp;', ' '), E'\u00A0', ' '),
            E'[\u200B\u200C\u200D\uFEFF\u00AD]', '', 'g'
        ),
        E'[ \t]{2,}', ' ', 'g'
    )
)
WHERE key = 'store_tagline' AND value IS NOT NULL;

UPDATE product
SET description = btrim(
    regexp_replace(
        regexp_replace(
            replace(replace(description, '&nbsp;', ' '), E'\u00A0', ' '),
            E'[\u200B\u200C\u200D\uFEFF\u00AD]', '', 'g'
        ),
        E'[ \t]{2,}', ' ', 'g'
    )
)
WHERE description IS NOT NULL;
