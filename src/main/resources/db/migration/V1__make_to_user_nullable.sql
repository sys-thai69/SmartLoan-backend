-- Make to_user column nullable for internal wallet transactions
ALTER TABLE wallet_transactions
ALTER COLUMN to_user DROP NOT NULL;
