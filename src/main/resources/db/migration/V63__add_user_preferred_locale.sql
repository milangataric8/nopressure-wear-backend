-- Backs Strategy B of backend-message-i18n-spec.md section 4.3: transactional emails are often
-- sent asynchronously (or triggered by someone other than the recipient, e.g. an admin changing
-- an order's status), so there is no Accept-Language header to read at send time. The recipient's
-- own language preference is persisted instead and read from the User row when an email goes out.
--
-- Existing rows default to 'sr' (the store's primary market) per the spec's explicit instruction.
ALTER TABLE users ADD COLUMN preferred_locale VARCHAR(5) NOT NULL DEFAULT 'sr';
