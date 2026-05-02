-- Add delivery fields to clients table (nullable, safe for existing rows)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'clients') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'clients' AND column_name = 'delivery_type') THEN
            ALTER TABLE clients ADD COLUMN delivery_type VARCHAR(30);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'clients' AND column_name = 'delivery_city') THEN
            ALTER TABLE clients ADD COLUMN delivery_city VARCHAR(255);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'clients' AND column_name = 'delivery_branch') THEN
            ALTER TABLE clients ADD COLUMN delivery_branch VARCHAR(100);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'clients' AND column_name = 'delivery_street') THEN
            ALTER TABLE clients ADD COLUMN delivery_street VARCHAR(255);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'clients' AND column_name = 'delivery_building') THEN
            ALTER TABLE clients ADD COLUMN delivery_building VARCHAR(50);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'clients' AND column_name = 'delivery_free_address') THEN
            ALTER TABLE clients ADD COLUMN delivery_free_address VARCHAR(500);
        END IF;
    END IF;
END $$;