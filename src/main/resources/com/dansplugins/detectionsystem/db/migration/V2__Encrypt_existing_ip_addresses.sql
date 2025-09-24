-- Migration to handle IP address encryption
-- This migration will be handled by the application code during startup
-- to encrypt existing plaintext IP addresses in the database

-- Increase the size of the address column to accommodate encrypted values
-- Encrypted Base64 strings will be longer than IP addresses
ALTER TABLE aaf_login_record ALTER COLUMN address VARCHAR(255);