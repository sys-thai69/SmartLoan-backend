-- Fix wallet_transactions type check constraint to allow RESERVED type
DO $$
BEGIN
    -- Drop the existing check constraint if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'wallet_transactions'
        AND constraint_name = 'wallet_transactions_type_check'
    ) THEN
        ALTER TABLE wallet_transactions DROP CONSTRAINT wallet_transactions_type_check;
    END IF;

    -- Add the updated check constraint that includes RESERVED
    ALTER TABLE wallet_transactions
    ADD CONSTRAINT wallet_transactions_type_check
    CHECK (type IN ('topup', 'transfer', 'auto_debit', 'refund', 'reserved'));

EXCEPTION WHEN OTHERS THEN
    -- If constraint already exists with correct values, continue
    NULL;
END $$;
