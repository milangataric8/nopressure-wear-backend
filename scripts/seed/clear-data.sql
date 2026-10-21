-- DELETES all users (including SUPER_ADMIN), products, orders and other shop data.
-- Preserves store_settings, filter_config, legal_content and Flyway history,
-- because nopressure.sql does not recreate those application defaults.
-- Uploaded image files are not deleted.
-- Stop the backend, run this entire file, then run nopressure.sql.
-- If the previous query failed in this session, execute ROLLBACK; first.

BEGIN;
SET LOCAL lock_timeout = '10s';

TRUNCATE TABLE
    public.order_item,
    public.orders,
    public.cart_item,
    public.cart,
    public.favorite,
    public.product_review,
    public.product_store,
    public.product_color_variant,
    public.product_image,
    public.product_variant,
    public.product,
    public.category,
    public.email_verification_token,
    public.address,
    public.users,
    public.coupon,
    public.banner,
    public.popup,
    public.store_location,
    public.notification,
    public.failed_email
RESTART IDENTITY;

-- Optional: uncomment ONLY if you also want to erase settings, filters and
-- legal content. Neither Flyway on normal startup nor nopressure.sql will
-- restore these defaults after truncation.
-- TRUNCATE TABLE public.store_settings, public.filter_config,
--     public.legal_content RESTART IDENTITY;

COMMIT;
