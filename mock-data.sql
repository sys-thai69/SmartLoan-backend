-- SmartLoan Mock Data Script
-- Insert test users
INSERT INTO users (id, name, email, phone_number, firebase_uid, password, role, trust_score, email_verified, phone_verified, created_at)
VALUES
  ('10000000-0000-0000-0000-000000000001', 'Alice Johnson', 'alice@example.com', '+1234567890', 'firebase_alice', 'hashed_password', 'USER', 95.0, true, true, NOW()),
  ('10000000-0000-0000-0000-000000000002', 'Bob Smith', 'bob@example.com', '+1234567891', 'firebase_bob', 'hashed_password', 'USER', 88.0, true, true, NOW()),
  ('10000000-0000-0000-0000-000000000003', 'Carol Davis', 'carol@example.com', '+1234567892', 'firebase_carol', 'hashed_password', 'USER', 92.0, true, true, NOW()),
  ('10000000-0000-0000-0000-000000000004', 'David Lee', 'david@example.com', '+1234567893', 'firebase_david', 'hashed_password', 'USER', 85.0, true, true, NOW()),
  ('10000000-0000-0000-0000-000000000005', 'Eve Wilson', 'eve@example.com', '+1234567894', 'firebase_eve', 'hashed_password', 'USER', 90.0, true, true, NOW()),
  ('10000000-0000-0000-0000-000000000006', 'Frank Brown', 'frank@example.com', '+1234567895', 'firebase_frank', 'hashed_password', 'USER', 87.0, true, true, NOW());

-- Insert wallets for each user (use these UUIDs or generate new ones)
INSERT INTO wallets (id, user_id, balance, currency, updated_at)
VALUES
  ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 5000.00, 'USD', NOW()),
  ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 3500.00, 'USD', NOW()),
  ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 7200.00, 'USD', NOW()),
  ('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000004', 2100.00, 'USD', NOW()),
  ('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000005', 8500.00, 'USD', NOW()),
  ('20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000006', 1500.00, 'USD', NOW());

-- Insert loans with different statuses
-- Loan 1: Alice lending to Bob (PENDING_ACCEPTANCE)
INSERT INTO loans (id, lender_id, borrower_id, principal, interest_rate, total_amount, installments, frequency, start_date, status, auto_debit, is_quick_lend, created_at)
VALUES
  ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', 500.00, 5.0, 525.00, 3, 'MONTHLY', '2026-03-28', 'PENDING_ACCEPTANCE', false, true, NOW());

-- Loan 2: Carol lending to David (ACTIVE)
INSERT INTO loans (id, lender_id, borrower_id, principal, interest_rate, total_amount, installments, frequency, start_date, status, auto_debit, is_quick_lend, created_at)
VALUES
  ('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000004', 1000.00, 3.0, 1030.00, 6, 'MONTHLY', '2026-02-28', 'ACTIVE', true, false, NOW() - INTERVAL '30 days');

-- Loan 3: Bob lending to Eve (COMPLETED)
INSERT INTO loans (id, lender_id, borrower_id, principal, interest_rate, total_amount, installments, frequency, start_date, status, auto_debit, is_quick_lend, created_at)
VALUES
  ('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000005', 2000.00, 4.0, 2080.00, 12, 'MONTHLY', '2025-04-01', 'COMPLETED', true, false, NOW() - INTERVAL '365 days');

-- Loan 4: David lending to Frank (OVERDUE)
INSERT INTO loans (id, lender_id, borrower_id, principal, interest_rate, total_amount, installments, frequency, start_date, status, auto_debit, is_quick_lend, created_at)
VALUES
  ('30000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000006', 800.00, 6.0, 848.00, 4, 'WEEKLY', '2025-12-01', 'OVERDUE', false, false, NOW() - INTERVAL '120 days');

-- Loan 5: Eve lending to Alice (ACTIVE)
INSERT INTO loans (id, lender_id, borrower_id, principal, interest_rate, total_amount, installments, frequency, start_date, status, auto_debit, is_quick_lend, created_at)
VALUES
  ('30000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', 1500.00, 2.5, 1537.50, 8, 'MONTHLY', '2026-01-28', 'ACTIVE', false, false, NOW() - INTERVAL '60 days');

-- Loan 6: Frank lending to Carol (DECLINED)
INSERT INTO loans (id, lender_id, borrower_id, principal, interest_rate, total_amount, installments, frequency, start_date, status, auto_debit, is_quick_lend, created_at)
VALUES
  ('30000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000003', 600.00, 7.0, 642.00, 2, 'MONTHLY', '2026-03-01', 'DECLINED', false, true, NOW() - INTERVAL '28 days');

-- Insert repayment schedules for active loans
-- Loan 2 (Carol -> David): ACTIVE
INSERT INTO repayment_schedules (id, loan_id, due_date, amount, paid_amount, status, created_at)
VALUES
  ('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002', '2026-02-28', 171.67, 171.67, 'PAID', NOW() - INTERVAL '30 days'),
  ('40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', '2026-03-28', 171.67, 0.00, 'PENDING', NOW()),
  ('40000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000002', '2026-04-28', 171.67, 0.00, 'PENDING', NOW() + INTERVAL '31 days'),
  ('40000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000002', '2026-05-28', 171.67, 0.00, 'PENDING', NOW() + INTERVAL '61 days'),
  ('40000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000002', '2026-06-28', 171.67, 0.00, 'PENDING', NOW() + INTERVAL '92 days'),
  ('40000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000002', '2026-07-28', 171.67, 0.00, 'PENDING', NOW() + INTERVAL '122 days');

-- Loan 5 (Eve -> Alice): ACTIVE
INSERT INTO repayment_schedules (id, loan_id, due_date, amount, paid_amount, status, created_at)
VALUES
  ('40000000-0000-0000-0000-000000000007', '30000000-0000-0000-0000-000000000005', '2026-02-28', 192.19, 192.19, 'PAID', NOW() - INTERVAL '60 days'),
  ('40000000-0000-0000-0000-000000000008', '30000000-0000-0000-0000-000000000005', '2026-03-28', 192.19, 0.00, 'PENDING', NOW()),
  ('40000000-0000-0000-0000-000000000009', '30000000-0000-0000-0000-000000000005', '2026-04-28', 192.19, 0.00, 'PENDING', NOW() + INTERVAL '31 days'),
  ('40000000-0000-0000-0000-000000000010', '30000000-0000-0000-0000-000000000005', '2026-05-28', 192.19, 0.00, 'PENDING', NOW() + INTERVAL '61 days'),
  ('40000000-0000-0000-0000-000000000011', '30000000-0000-0000-0000-000000000005', '2026-06-28', 192.19, 0.00, 'PENDING', NOW() + INTERVAL '92 days'),
  ('40000000-0000-0000-0000-000000000012', '30000000-0000-0000-0000-000000000005', '2026-07-28', 192.19, 0.00, 'PENDING', NOW() + INTERVAL '122 days'),
  ('40000000-0000-0000-0000-000000000013', '30000000-0000-0000-0000-000000000005', '2026-08-28', 192.19, 0.00, 'PENDING', NOW() + INTERVAL '153 days'),
  ('40000000-0000-0000-0000-000000000014', '30000000-0000-0000-0000-000000000005', '2026-09-28', 192.88, 0.00, 'PENDING', NOW() + INTERVAL '184 days');

-- Insert payments for completed loan
-- Loan 3 (Bob -> Eve): COMPLETED
INSERT INTO payments (id, loan_id, amount, payment_type, transaction_date, created_at)
VALUES
  ('50000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000003', 2080.00, 'REPAYMENT', '2025-12-31', NOW() - INTERVAL '90 days');

-- Insert payment for overdue loan (partial)
-- Loan 4 (David -> Frank): OVERDUE
INSERT INTO payments (id, loan_id, amount, payment_type, transaction_date, created_at)
VALUES
  ('50000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000004', 212.00, 'REPAYMENT', '2026-01-15', NOW() - INTERVAL '72 days');
