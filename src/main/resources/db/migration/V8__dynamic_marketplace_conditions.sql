ALTER TABLE products
    ALTER COLUMN condition DROP DEFAULT;

ALTER TABLE products
    ALTER COLUMN condition TYPE VARCHAR(30) USING condition::text;

ALTER TABLE products
    ALTER COLUMN condition SET DEFAULT 'GOOD';
