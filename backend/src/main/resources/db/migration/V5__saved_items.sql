-- Saved items catalog for order item autocomplete
CREATE TABLE IF NOT EXISTS saved_items (
    id           VARCHAR(36) PRIMARY KEY,
    name         VARCHAR(255) NOT NULL UNIQUE,
    unit         VARCHAR(20)  NOT NULL DEFAULT 'шт.',
    default_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    created_at   TIMESTAMP NOT NULL
);

-- Add name and unit columns to order_items
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS name VARCHAR(255);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS unit VARCHAR(20) NOT NULL DEFAULT 'шт.';
