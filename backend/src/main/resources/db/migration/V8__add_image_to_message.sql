ALTER TABLE message ADD COLUMN message_type VARCHAR(32) NOT NULL DEFAULT 'text' AFTER content;
ALTER TABLE message ADD COLUMN image_url VARCHAR(500) NULL AFTER message_type;
