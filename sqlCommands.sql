SELECT * FROM accounts;

SELECT * FROM clients;

SELECT * FROM clients WHERE name = 'Yoski';

SELECT * FROM account_clients WHERE account_id = 16;

SELECT * FROM business_checking_accounts;

-- 1 VIEWS
SELECT * FROM v_all_account_details;

-- 2 TRIGGER 
SELECT * FROM client_rank_audit_log;

-- 3 INDEX 
SELECT * FROM accounts WHERE account_number = 4;

