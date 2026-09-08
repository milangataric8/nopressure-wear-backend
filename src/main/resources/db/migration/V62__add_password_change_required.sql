-- Forces a fresh account to change its password before it can do anything else: set to TRUE
-- whenever someone other than the account owner sets its password (SUPER_ADMIN creating an
-- employee, or SUPER_ADMIN resetting another staff member's password via the edit form) — that
-- person now knows the password too. Enforced by PasswordChangeRequiredFilter, not the frontend.
--
-- Existing accounts default to FALSE — nobody currently working gets locked out by this deploy.
ALTER TABLE users ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;
