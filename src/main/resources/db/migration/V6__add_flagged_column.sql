-- Add flagged column to loans table
ALTER TABLE loans ADD COLUMN IF NOT EXISTS flagged BOOLEAN NOT NULL DEFAULT false;
