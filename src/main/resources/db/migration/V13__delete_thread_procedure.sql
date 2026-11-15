-- Deleting a post or comment with its whole reply tree, however deep.
--
-- ON DELETE CASCADE alone cannot do it: InnoDB refuses cascades nested more
-- than 15 levels, so deleting the top of a longer chain of replies fails.
-- The procedure deletes every row of the tree itself, newest first: a reply
-- is always newer than what it answers (a larger id), so each row goes after
-- all of its replies and the cascade never nests - it only takes the row's
-- publication_media. It stays as a safety net for a reply written meanwhile.
--
-- The tree is collected with a recursive query that stops by itself after
-- 999 levels, below MySQL's limit of 1000 recursion steps
-- (cte_max_recursion_depth, left as it is); a deeper tree is walked on from
-- the nodes at that level, so any depth works. A real thread takes one step.
--
-- The media files of the deleted rows live on disk, which the database
-- cannot clean up: their ids are the procedure's result, for the
-- application to remove the files once the transaction commits.
DELIMITER //

CREATE PROCEDURE delete_thread(IN p_root_id BIGINT)
BEGIN
    DECLARE v_step INT DEFAULT 0;
    DECLARE v_frontier INT DEFAULT 1;

    -- Left over only if an earlier call on this connection failed midway.
    DROP TEMPORARY TABLE IF EXISTS thread_rows, thread_start, thread_media;

    -- Every row of the tree; step and depth tell where the next step starts.
    CREATE TEMPORARY TABLE thread_rows (
        id    BIGINT NOT NULL PRIMARY KEY,
        step  INT    NOT NULL,
        depth INT    NOT NULL
    );
    -- Where the current step starts.
    CREATE TEMPORARY TABLE thread_start (id BIGINT NOT NULL PRIMARY KEY);
    CREATE TEMPORARY TABLE thread_media (file_id VARCHAR(64) NOT NULL);

    INSERT INTO thread_start SELECT id FROM publications WHERE id = p_root_id;

    WHILE v_frontier > 0 DO
        -- The start nodes of a later step are already collected (at depth 999
        -- of the step before); IGNORE keeps their first entry.
        INSERT IGNORE INTO thread_rows (id, step, depth)
        WITH RECURSIVE tree (id, depth) AS (
            SELECT id, 0 FROM thread_start
            UNION ALL
            SELECT p.id, t.depth + 1
            FROM publications p
            JOIN tree t ON p.parent_id = t.id
            WHERE t.depth < 999
        )
        SELECT id, v_step, depth FROM tree;

        DELETE FROM thread_start;
        INSERT INTO thread_start SELECT id FROM thread_rows WHERE step = v_step AND depth = 999;
        SET v_frontier = ROW_COUNT();
        SET v_step = v_step + 1;
    END WHILE;

    INSERT INTO thread_media
    SELECT pm.file_id FROM publication_media pm JOIN thread_rows r ON r.id = pm.publication_id;

    DELETE FROM publications WHERE id IN (SELECT id FROM thread_rows) ORDER BY id DESC;

    -- The result comes last, so that any error above reaches the caller
    -- instead of a result.
    SELECT file_id FROM thread_media;

    DROP TEMPORARY TABLE thread_rows, thread_start, thread_media;
END //

DELIMITER ;
