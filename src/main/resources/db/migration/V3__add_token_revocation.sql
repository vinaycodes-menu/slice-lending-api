ALTER TABLE email_verification_tokens
ADD COLUMN revoked_at TIMESTAMPTZ;
