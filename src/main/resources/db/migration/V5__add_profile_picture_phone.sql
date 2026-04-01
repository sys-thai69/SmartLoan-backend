-- Add profilePicture and ensure phoneNumber is in users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_picture TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20) UNIQUE;

-- If phone_number already exists, no need to add it again
