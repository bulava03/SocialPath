-- Phone numbers are confirmed with a one-time code, and a confirmed number
-- signs its owner in instead of the login.
--
-- * phone_verified: the code sent to the number was entered. Numbers stored
--   before this migration start unconfirmed; their owners confirm them from
--   the Security page.
-- * pending_since: set while a registration waits for its code. Such an
--   account cannot sign in yet and is removed if nobody confirms it within
--   a day, so an abandoned sign-up does not hold its login for ever.
-- * verified_phone: the number while it is confirmed, NULL otherwise, so the
--   unique key lets any number be typed by several accounts but confirmed
--   by only one of them - the one it signs in.
ALTER TABLE users
    ADD COLUMN phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN pending_since DATETIME(6) NULL,
    ADD KEY idx_users_pending (pending_since);

ALTER TABLE users
    ADD COLUMN verified_phone VARCHAR(16) AS (IF(phone_verified, phone, NULL)) STORED,
    ADD CONSTRAINT uk_users_verified_phone UNIQUE (verified_phone),
    ADD CONSTRAINT chk_users_verified_phone CHECK (NOT phone_verified OR phone IS NOT NULL);

-- The code last sent to a user, at most one per user: sending again replaces
-- it and keeps the id, which is what the confirmation page's address holds.
-- Only a BCrypt hash of the code is kept. phone is the number the code went
-- to: a code does not confirm a number the profile has changed to since.
CREATE TABLE phone_codes (
    id         VARCHAR(36) NOT NULL,
    user_login VARCHAR(20) NOT NULL,
    phone      VARCHAR(16) NOT NULL,
    code_hash  VARCHAR(72) NOT NULL,
    attempts   INT         NOT NULL DEFAULT 0,
    sent_at    DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone_codes_user (user_login),
    KEY idx_phone_codes_expires (expires_at),
    CONSTRAINT fk_phone_codes_user
        FOREIGN KEY (user_login) REFERENCES users (login) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
