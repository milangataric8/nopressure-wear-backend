ALTER TABLE orders ADD COLUMN shipping_street VARCHAR(255);
ALTER TABLE orders ADD COLUMN shipping_city VARCHAR(100);
ALTER TABLE orders ADD COLUMN shipping_postal_code VARCHAR(20);
ALTER TABLE orders ADD COLUMN shipping_country VARCHAR(100);
ALTER TABLE orders ADD COLUMN customer_first_name VARCHAR(100);
ALTER TABLE orders ADD COLUMN customer_last_name VARCHAR(100);
ALTER TABLE orders ADD COLUMN customer_email VARCHAR(255);