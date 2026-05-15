ALTER TABLE orders ADD COLUMN order_code VARCHAR(20) UNIQUE;
ALTER TABLE orders ADD COLUMN customer_full_name VARCHAR(255);
UPDATE orders SET customer_full_name = customer_first_name || ' ' || customer_last_name;
ALTER TABLE orders DROP COLUMN customer_first_name;
ALTER TABLE orders DROP COLUMN customer_last_name;