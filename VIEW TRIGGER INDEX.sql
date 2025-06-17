-- =====================================================
-- Advanced Database Features for Banking Application
-- =====================================================

-- -----------------------------------------------------
-- Sub-Task 2.1: Create Database VIEW
-- -----------------------------------------------------

-- Create a comprehensive view that encapsulates the complex LEFT JOIN query
-- This view consolidates all account types and their specific attributes
CREATE OR REPLACE VIEW v_all_account_details AS
SELECT 
    -- Base account fields
    a.account_id, 
    a.account_number, 
    a.account_type, 
    a.date_opened, 
    a.bank_id, 
    a.balance, 
    a.manager_name,
    
    -- Regular checking account fields
    rca.credit_limit as rca_credit_limit, 
    rca.profit as rca_profit,
    
    -- Business checking account fields  
    bca.credit_limit as bca_credit_limit, 
    bca.business_revenue, 
    bca.profit as bca_profit, 
    bca.management_fee as bca_management_fee,
    
    -- Mortgage account fields
    ma.original_mortgage_amount, 
    ma.monthly_payment, 
    ma.years as ma_years, 
    ma.profit as ma_profit, 
    ma.management_fee as ma_management_fee,
    
    -- Savings account fields
    sa.deposit_amount, 
    sa.years as sa_years
FROM accounts a 
LEFT JOIN regular_checking_accounts rca ON a.account_id = rca.account_id 
LEFT JOIN business_checking_accounts bca ON a.account_id = bca.account_id 
LEFT JOIN mortgage_accounts ma ON a.account_id = ma.account_id 
LEFT JOIN savings_accounts sa ON a.account_id = sa.account_id;

-- Add a comment to document the view's purpose
COMMENT ON VIEW v_all_account_details IS 'Comprehensive view of all account types with their specific attributes using LEFT JOINs for Class Table Inheritance pattern';

-- -----------------------------------------------------
-- Sub-Task 2.2: Create Audit Table and Trigger System
-- -----------------------------------------------------

-- Create the audit table for tracking client rank changes
CREATE TABLE IF NOT EXISTS client_rank_audit_log (
    log_id SERIAL PRIMARY KEY,
    client_id INT NOT NULL,
    old_rank INT NOT NULL,
    new_rank INT NOT NULL,
    change_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Add foreign key constraint for data integrity
    CONSTRAINT fk_audit_client_id 
        FOREIGN KEY (client_id) 
        REFERENCES clients(client_id) 
        ON DELETE CASCADE,
        
    -- Add check constraints for valid rank values
    CONSTRAINT chk_old_rank_valid CHECK (old_rank >= 0 AND old_rank <= 10),
    CONSTRAINT chk_new_rank_valid CHECK (new_rank >= 0 AND new_rank <= 10)
);

-- Add index on client_id for faster audit queries
CREATE INDEX IF NOT EXISTS idx_audit_client_id ON client_rank_audit_log(client_id);

-- Add index on change_time for chronological queries
CREATE INDEX IF NOT EXISTS idx_audit_change_time ON client_rank_audit_log(change_time);

-- Add comment to document the audit table's purpose
COMMENT ON TABLE client_rank_audit_log IS 'Audit log for tracking all changes to client rank values with timestamps';

-- Create the trigger function that will log rank changes
CREATE OR REPLACE FUNCTION log_client_rank_change()
RETURNS TRIGGER AS $$
BEGIN
    -- Only log if the rank_value actually changed
    IF OLD.rank_value IS DISTINCT FROM NEW.rank_value THEN
        INSERT INTO client_rank_audit_log (
            client_id, 
            old_rank, 
            new_rank, 
            change_time
        ) VALUES (
            NEW.client_id,
            OLD.rank_value,
            NEW.rank_value,
            CURRENT_TIMESTAMP
        );
        
        -- Log the change for debugging (optional)
        RAISE NOTICE 'Client rank change logged: Client ID %, Old Rank %, New Rank %', 
            NEW.client_id, OLD.rank_value, NEW.rank_value;
    END IF;
    
    -- Return NEW to allow the update to proceed
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Add comment to document the function's purpose
COMMENT ON FUNCTION log_client_rank_change() IS 'Trigger function that logs client rank changes to the audit table';

-- Create the trigger that calls the function after UPDATE operations
CREATE OR REPLACE TRIGGER trigger_client_rank_audit
    AFTER UPDATE OF rank_value ON clients
    FOR EACH ROW
    EXECUTE FUNCTION log_client_rank_change();

-- Add comment to document the trigger's purpose
COMMENT ON TRIGGER trigger_client_rank_audit ON clients IS 'Trigger that automatically logs rank changes to the audit table';

-- -----------------------------------------------------
-- Sub-Task 2.3: Create Performance Index
-- -----------------------------------------------------

-- Create index on account_number for faster lookups
-- This is the primary business key used for account searches
CREATE INDEX IF NOT EXISTS idx_accounts_account_number 
ON accounts(account_number);

-- Add comment to document the index's purpose
COMMENT ON INDEX idx_accounts_account_number IS 'Index on account_number to optimize account lookup queries by business key';

-- Additional performance indexes that would be beneficial
-- (These are optional but recommended for a production system)

-- Index on account_type for faster filtering by type
CREATE INDEX IF NOT EXISTS idx_accounts_account_type 
ON accounts(account_type);

-- Index on bank_id for multi-bank scenarios
CREATE INDEX IF NOT EXISTS idx_accounts_bank_id 
ON accounts(bank_id);

-- Composite index for common query patterns
CREATE INDEX IF NOT EXISTS idx_accounts_type_number 
ON accounts(account_type, account_number);

-- -----------------------------------------------------
-- Verification Queries (for testing the new features)
-- -----------------------------------------------------

-- Test the view
-- SELECT * FROM v_all_account_details WHERE account_type = 'Business Checking Account';

-- Test the audit system (update a client rank to see the trigger in action)
-- UPDATE clients SET rank_value = 5 WHERE client_id = 1;
-- SELECT * FROM client_rank_audit_log;

-- Check index usage
-- EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM accounts WHERE account_number = 12345;

-- -----------------------------------------------------
-- Cleanup Commands (for development/testing)
-- -----------------------------------------------------

-- Uncomment these lines if you need to drop and recreate the objects:
-- DROP TRIGGER IF EXISTS trigger_client_rank_audit ON clients;
-- DROP FUNCTION IF EXISTS log_client_rank_change();
-- DROP TABLE IF EXISTS client_rank_audit_log CASCADE;
-- DROP VIEW IF EXISTS v_all_account_details;
-- DROP INDEX IF EXISTS idx_accounts_account_number;

-- End of script