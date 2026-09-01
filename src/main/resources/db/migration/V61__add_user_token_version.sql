-- Backs JWT invalidation on role change: bumping token_version makes every JWT issued
-- before the bump fail validation (JwtUtil compares the token's "tv" claim to this column),
-- forcing a re-login instead of letting a stale token keep its old privileges (or, for a
-- promoted user, wait out the token's remaining lifetime before gaining the new ones).
--
-- One-time side effect of this deploy: every token already issued has no "tv" claim, so
-- every currently logged-in user is signed out once and must log back in.
ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0;
