-- What a group is about (description, topic, link) and when it was created.
ALTER TABLE social_groups
    ADD COLUMN description VARCHAR(1000) NULL,
    ADD COLUMN category    VARCHAR(20)   NULL,
    ADD COLUMN website     VARCHAR(255)  NULL,
    ADD COLUMN created_at  DATETIME(6)   NULL,
    ADD CONSTRAINT chk_groups_category CHECK (category IN (
        'ARTS', 'BOOKS', 'BUSINESS', 'EDUCATION', 'FOOD', 'GAMES', 'HEALTH',
        'MUSIC', 'SCIENCE', 'SPORTS', 'TECHNOLOGY', 'TRAVEL', 'OTHER'));

-- Existing groups never recorded their creation time; it is estimated from
-- two facts the data does hold:
--   * a group exists before its first post, so it was created no later than
--     the earliest publication in it;
--   * ids are handed out in creation order, so a group was also created no
--     later than any group with a higher id.
-- The estimate for a group is therefore the earliest first post among it and
-- all groups after it: the latest time it can have been created that agrees
-- with both. Groups with no posts at or after them get the migration time.
UPDATE social_groups g
JOIN (
    SELECT id,
           MIN(first_post) OVER (ORDER BY id DESC) AS created_by
    FROM (
        SELECT sg.id,
               (SELECT MIN(p.created_at) FROM publications p WHERE p.group_id = sg.id) AS first_post
        FROM social_groups sg
    ) first_posts
) estimate ON estimate.id = g.id
SET g.created_at = COALESCE(estimate.created_by, NOW(6));

-- From now on the database stamps new groups itself (the application also
-- sets the time explicitly).
ALTER TABLE social_groups MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
