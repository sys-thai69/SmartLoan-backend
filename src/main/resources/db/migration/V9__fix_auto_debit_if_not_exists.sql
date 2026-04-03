-- Safely ensure auto_debit_schedules table exists with proper schema
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_name = 'auto_debit_schedules'
    ) THEN
        CREATE TABLE auto_debit_schedules (
            id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
            loan_id VARCHAR(36) NOT NULL UNIQUE REFERENCES loans(id) ON DELETE CASCADE,
            schedule_id VARCHAR(36) NOT NULL,
            amount DECIMAL(14,2) NOT NULL,
            next_debit_date DATE NOT NULL,
            status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
            failure_count INT DEFAULT 0,
            last_attempt TIMESTAMP,
            last_failure_reason TEXT,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP
        );

        CREATE INDEX idx_auto_debit_schedules_loan_id ON auto_debit_schedules(loan_id);
        CREATE INDEX idx_auto_debit_schedules_status ON auto_debit_schedules(status);
        CREATE INDEX idx_auto_debit_schedules_next_debit ON auto_debit_schedules(next_debit_date);
    END IF;
END $$;
