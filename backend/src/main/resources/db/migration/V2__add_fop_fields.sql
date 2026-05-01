-- Add FOP fields to business_profiles (only if the table already exists from a prior deployment)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'business_profiles') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'business_profiles' AND column_name = 'ipn') THEN
            ALTER TABLE business_profiles ADD COLUMN ipn VARCHAR(20);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'business_profiles' AND column_name = 'mfo') THEN
            ALTER TABLE business_profiles ADD COLUMN mfo VARCHAR(10);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'business_profiles' AND column_name = 'certificate_number') THEN
            ALTER TABLE business_profiles ADD COLUMN certificate_number VARCHAR(255);
        END IF;
    END IF;
END $$;

-- Add seller snapshot fields to invoices (only if the table already exists from a prior deployment)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'invoices') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'invoices' AND column_name = 'seller_tax_note') THEN
            ALTER TABLE invoices ADD COLUMN seller_tax_note TEXT;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'invoices' AND column_name = 'seller_mfo') THEN
            ALTER TABLE invoices ADD COLUMN seller_mfo VARCHAR(10);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'invoices' AND column_name = 'seller_ipn') THEN
            ALTER TABLE invoices ADD COLUMN seller_ipn VARCHAR(20);
        END IF;
    END IF;
END $$;
