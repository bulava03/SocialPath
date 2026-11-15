-- Every comment knows the post at the top of its thread (root_id), so the
-- place a thread lives in - a user page or a group, stored on the post -
-- is one lookup away instead of a walk up the tree, one query per level.
-- Posts have no root (NULL); a new comment copies its parent's root, or the
-- parent itself when the parent is the post.
ALTER TABLE publications
    ADD COLUMN root_id BIGINT NULL AFTER parent_id;

-- Existing comments get their root a level at a time: first the direct
-- replies to posts, then the replies to those, and so on until a level comes
-- back empty. A loop rather than one recursive query, so that no limit on
-- recursion depth applies; it runs once.
DELIMITER //

CREATE PROCEDURE fill_publication_roots()
BEGIN
    UPDATE publications c
    JOIN publications p ON c.parent_id = p.id
    SET c.root_id = p.id
    WHERE p.parent_id IS NULL;

    REPEAT
        UPDATE publications c
        JOIN publications p ON c.parent_id = p.id
        SET c.root_id = p.root_id
        WHERE c.root_id IS NULL AND p.root_id IS NOT NULL;
    UNTIL ROW_COUNT() = 0 END REPEAT;
END //

DELIMITER ;

CALL fill_publication_roots();
DROP PROCEDURE fill_publication_roots;

-- A comment always has a root, a post never. The cascade follows the one on
-- parent_id: delete_thread deletes a tree newest first, so by the time a
-- post goes nothing refers to it any more.
ALTER TABLE publications
    ADD KEY idx_publications_root (root_id),
    ADD CONSTRAINT fk_publications_root
        FOREIGN KEY (root_id) REFERENCES publications (id) ON DELETE CASCADE,
    ADD CONSTRAINT chk_publications_root
        CHECK ((parent_id IS NULL) = (root_id IS NULL));
