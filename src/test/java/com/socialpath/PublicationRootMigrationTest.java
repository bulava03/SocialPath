package com.socialpath;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * V14 gives every existing comment the post at the top of its thread. This
 * runs the real migration over rows written by the V13 schema, in a database
 * of its own on the shared test MySQL.
 */
class PublicationRootMigrationTest extends MigrationTest {

    private long publish(Long parentId, Long groupId) {
        jdbc.update("""
                INSERT INTO publications (author_login, parent_id, group_id, text, created_at)
                VALUES ('alex', ?, ?, 'text', NOW())
                """, parentId, groupId);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long rootOf(long id) {
        return jdbc.queryForObject("SELECT root_id FROM publications WHERE id = ?", Long.class, id);
    }

    @Test
    void existingComments_GetThePostOfTheirThread_AtAnyDepth() {
        migrateTo("13");
        jdbc.update("""
                INSERT INTO users (login, password, first_name, email, phone)
                VALUES ('alex', 'hash', 'Alex', 'alex@example.com', '+12015550123')
                """);
        jdbc.update("INSERT INTO social_groups (name, owner_login) VALUES ('Group', 'alex')");
        long groupId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        long userPost = publish(null, null);
        long comment = publish(userPost, null);
        long sibling = publish(userPost, null);
        // A chain deeper than the database's cascade and recursion limits.
        long deepest = comment;
        for (int depth = 2; depth <= 1200; depth++) {
            deepest = publish(deepest, null);
        }
        long groupPost = publish(null, groupId);
        long groupComment = publish(groupPost, null);
        long groupReply = publish(groupComment, null);

        migrateTo("14");

        assertNull(rootOf(userPost));
        assertNull(rootOf(groupPost));
        assertEquals(userPost, rootOf(comment));
        assertEquals(userPost, rootOf(sibling));
        assertEquals(userPost, rootOf(deepest));
        assertEquals(groupPost, rootOf(groupComment));
        assertEquals(groupPost, rootOf(groupReply));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM publications WHERE parent_id IS NOT NULL AND root_id IS NULL", Integer.class));
    }
}
