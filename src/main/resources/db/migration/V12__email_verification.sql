-- Email addresses are confirmed by a link sent to them, and a confirmed
-- address signs its owner in like the login. Each user also chooses who sees
-- the address, as with the phone number.
--
-- * email_verified: the link sent to the address was opened. Addresses
--   stored before this migration start unconfirmed.
-- * email_visibility: who sees the address once it is confirmed; until then
--   only the owner does, whatever is stored here.
-- * verified_email: the address while it is confirmed, NULL otherwise, so the
--   unique key lets an address be typed by several accounts but confirmed by
--   only one. The column collation ignores case, as mail providers do.
ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN email_visibility VARCHAR(10) NOT NULL DEFAULT 'FRIENDS',
    ADD CONSTRAINT chk_users_email_visibility
        CHECK (email_visibility IN ('EVERYONE', 'FRIENDS', 'NOBODY'));

ALTER TABLE users
    ADD COLUMN verified_email VARCHAR(100) AS (IF(email_verified, email, NULL)) STORED,
    ADD CONSTRAINT uk_users_verified_email UNIQUE (verified_email);

-- The confirmation link last sent to a user, one row per user: sending again
-- overwrites it in place (keyed by the login, so concurrent sign-ups never
-- lock each other's gaps the way delete-then-insert would). Only a SHA-256
-- hash of the link's token is kept, so the table cannot be used to confirm
-- anything. email is the address the link went to: it does not confirm an
-- address the profile has changed to since.
CREATE TABLE email_links (
    user_login VARCHAR(20)  NOT NULL,
    token_hash CHAR(64)     NOT NULL,
    email      VARCHAR(100) NOT NULL,
    sent_at    DATETIME(6)  NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (user_login),
    UNIQUE KEY uk_email_links_token (token_hash),
    KEY idx_email_links_expires (expires_at),
    CONSTRAINT fk_email_links_user
        FOREIGN KEY (user_login) REFERENCES users (login) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
