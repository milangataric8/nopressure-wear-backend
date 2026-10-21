-- Alternative to clearing data: fixes the reported category_pkey collision.
-- Keeps every row. Stop the backend before running.
-- If the previous query failed in this session, execute ROLLBACK; first.
BEGIN;
SET LOCAL lock_timeout = '10s';
LOCK TABLE public.category IN ACCESS EXCLUSIVE MODE;
SELECT setval(
    pg_get_serial_sequence('public.category', 'id'),
    COALESCE((SELECT MAX(id) FROM public.category), 1),
    EXISTS (SELECT 1 FROM public.category)
);
COMMIT;
