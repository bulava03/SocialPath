-- One row per signed-in browser. The JWT carries the row id, so a token is
-- accepted only while its row exists: deleting the row signs that browser out
-- (logout, "end session", a password change ending the other sessions).
-- expires_at moves forward while the session is used; rows past it are
-- removed by a scheduled cleanup.
CREATE TABLE user_sessions (
    id           VARCHAR(36)  NOT NULL,
    user_login   VARCHAR(20)  NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    last_seen_at DATETIME(6)  NOT NULL,
    expires_at   DATETIME(6)  NOT NULL,
    ip           VARCHAR(45)  NOT NULL,
    user_agent   VARCHAR(255) NULL,
    location     VARCHAR(150) NULL,
    PRIMARY KEY (id),
    KEY idx_user_sessions_user (user_login),
    KEY idx_user_sessions_expires (expires_at),
    CONSTRAINT fk_user_sessions_user
        FOREIGN KEY (user_login) REFERENCES users (login) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
