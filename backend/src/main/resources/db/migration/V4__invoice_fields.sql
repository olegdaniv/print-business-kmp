-- Add new invoice fields (nullable / with defaults, safe for existing rows)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'invoices') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'invoices' AND column_name = 'client_id') THEN
            ALTER TABLE invoices ADD COLUMN client_id VARCHAR(36);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'invoices' AND column_name = 'valid_until') THEN
            ALTER TABLE invoices ADD COLUMN valid_until TIMESTAMP;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'invoices' AND column_name = 'payer') THEN
            ALTER TABLE invoices ADD COLUMN payer VARCHAR(255) NOT NULL DEFAULT 'той самий';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'invoices' AND column_name = 'order_ref') THEN
            ALTER TABLE invoices ADD COLUMN order_ref VARCHAR(255) NOT NULL DEFAULT 'Без замовлення';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'invoices' AND column_name = 'discount_amount') THEN
            ALTER TABLE invoices ADD COLUMN discount_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0;
        END IF;
    END IF;
END $$;