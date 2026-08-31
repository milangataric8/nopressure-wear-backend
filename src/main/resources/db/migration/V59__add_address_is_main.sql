-- Main shipping address per user. Backs the cart's "set as main address" checkbox,
-- which until now was sent as a flag with no matching column and silently dropped.
ALTER TABLE address ADD COLUMN is_main BOOLEAN NOT NULL DEFAULT false;

-- At most one main address per user, enforced in the database. A partial index is used
-- on purpose: a plain UNIQUE (user_id, is_main) would also forbid two non-main addresses.
CREATE UNIQUE INDEX uk_address_one_main_per_user
    ON address (user_id)
    WHERE is_main = true;
