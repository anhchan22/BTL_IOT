ALTER TABLE devices
    ADD COLUMN is_online BOOLEAN NOT NULL DEFAULT FALSE AFTER last_confirmed_status;
