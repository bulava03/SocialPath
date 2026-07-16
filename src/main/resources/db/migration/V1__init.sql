-- SocialPath schema, MySQL 8 (utf8mb4 everywhere).
--
-- Design notes relative to the MongoDB edition this project was ported from:
-- * user.login is the natural primary key: immutable, embedded in JWTs and
--   URLs, and referenced by every relation.
-- * The document model's embedded arrays become join tables (friendships,
--   friend_invites, group_members, group_admins, publication_media).
-- * Friendship is stored as mirrored rows (one per direction), preserving the
--   invariant the service layer maintained in MongoDB.
-- * Comments are publications with a parent_id (adjacency list); ON DELETE
--   CASCADE keeps subtrees consistent, while media file cleanup is done by
--   the application, which walks the tree before deleting.

CREATE TABLE users (
    login         VARCHAR(20)  NOT NULL,
    password      VARCHAR(72)  NOT NULL,
    image_id      VARCHAR(64)  NULL,
    first_name    VARCHAR(50)  NOT NULL,
    last_name     VARCHAR(50)  NULL,
    email         VARCHAR(100) NOT NULL,
    phone_number  VARCHAR(20)  NOT NULL,
    date_of_birth DATETIME(6)  NULL,
    country       VARCHAR(255) NULL,
    region        VARCHAR(255) NULL,
    city          VARCHAR(255) NULL,
    education     VARCHAR(255) NULL,
    workplace     VARCHAR(255) NULL,
    ban           DATETIME(6)  NULL,
    PRIMARY KEY (login)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE friendships (
    user_login   VARCHAR(20) NOT NULL,
    friend_login VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_login, friend_login),
    CONSTRAINT fk_friendships_user
        FOREIGN KEY (user_login) REFERENCES users (login) ON DELETE CASCADE,
    CONSTRAINT fk_friendships_friend
        FOREIGN KEY (friend_login) REFERENCES users (login) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE friend_invites (
    user_login    VARCHAR(20) NOT NULL,
    inviter_login VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_login, inviter_login),
    CONSTRAINT fk_invites_user
        FOREIGN KEY (user_login) REFERENCES users (login) ON DELETE CASCADE,
    CONSTRAINT fk_invites_inviter
        FOREIGN KEY (inviter_login) REFERENCES users (login) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- "groups" is a reserved word in MySQL 8, hence social_groups.
CREATE TABLE social_groups (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    image_id    VARCHAR(64) NULL,
    name        VARCHAR(50) NOT NULL,
    owner_login VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_groups_name (name),
    CONSTRAINT fk_groups_owner
        FOREIGN KEY (owner_login) REFERENCES users (login)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE group_members (
    group_id     BIGINT      NOT NULL,
    member_login VARCHAR(20) NOT NULL,
    PRIMARY KEY (group_id, member_login),
    KEY idx_group_members_login (member_login),
    CONSTRAINT fk_members_group
        FOREIGN KEY (group_id) REFERENCES social_groups (id) ON DELETE CASCADE,
    CONSTRAINT fk_members_user
        FOREIGN KEY (member_login) REFERENCES users (login) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE group_admins (
    group_id    BIGINT      NOT NULL,
    admin_login VARCHAR(20) NOT NULL,
    PRIMARY KEY (group_id, admin_login),
    CONSTRAINT fk_admins_group
        FOREIGN KEY (group_id) REFERENCES social_groups (id) ON DELETE CASCADE,
    CONSTRAINT fk_admins_user
        FOREIGN KEY (admin_login) REFERENCES users (login) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Publications and comments in one table: a comment is a publication with a
-- parent_id. Placement: user-page post (author_login, both group_id and
-- parent_id NULL), group post (group_id set), comment (parent_id set).
CREATE TABLE publications (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    author_login VARCHAR(20) NOT NULL,
    group_id     BIGINT      NULL,
    parent_id    BIGINT      NULL,
    text         TEXT        NULL,
    created_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_publications_author (author_login, parent_id),
    KEY idx_publications_group (group_id, parent_id),
    KEY idx_publications_parent (parent_id),
    CONSTRAINT fk_publications_author
        FOREIGN KEY (author_login) REFERENCES users (login),
    CONSTRAINT fk_publications_group
        FOREIGN KEY (group_id) REFERENCES social_groups (id) ON DELETE CASCADE,
    CONSTRAINT fk_publications_parent
        FOREIGN KEY (parent_id) REFERENCES publications (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE publication_media (
    publication_id BIGINT      NOT NULL,
    media_order    INT         NOT NULL,
    file_id        VARCHAR(64) NOT NULL,
    PRIMARY KEY (publication_id, media_order),
    CONSTRAINT fk_media_publication
        FOREIGN KEY (publication_id) REFERENCES publications (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Moderation reports. The id_* columns are free-form report payload, not
-- foreign keys: the front-end sends the sentinel value "publications" in
-- id_publication for top-level publications.
CREATE TABLE reports (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    author         VARCHAR(20)  NULL,
    date           DATETIME(6)  NULL,
    type           VARCHAR(30)  NULL,
    subject        TEXT         NULL,
    id_publication VARCHAR(64)  NULL,
    id_comment     VARCHAR(64)  NULL,
    id_user        VARCHAR(20)  NULL,
    id_group       VARCHAR(64)  NULL,
    status         VARCHAR(20)  NULL,
    result         TEXT         NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE admins (
    login     VARCHAR(20) NOT NULL,
    password  VARCHAR(72) NULL,
    report_id BIGINT      NULL,
    PRIMARY KEY (login)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
