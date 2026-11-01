package com.socialpath;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V8 estimates when existing groups were created from data that existed
 * before the column did. This runs the real migration over rows written by
 * the V7 schema, in a database of its own on the shared test MySQL.
 */
class GroupDetailsMigrationTest extends MigrationTest {

    private static final LocalDateTime NINE = LocalDateTime.of(2026, 3, 1, 9, 0);
    private static final LocalDateTime TEN = NINE.plusHours(1);
    private static final LocalDateTime ELEVEN = NINE.plusHours(2);

    private long group() {
        jdbc.update("INSERT INTO social_groups (name, owner_login) VALUES ('Group', 'anna')");
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void post(long groupId, LocalDateTime at) {
        jdbc.update("INSERT INTO publications (author_login, group_id, text, created_at) VALUES ('anna', ?, 'post', ?)",
                groupId, Timestamp.valueOf(at));
    }

    /** The database's clock, which the migration uses; it need not match the JVM's time zone. */
    private LocalDateTime databaseNow() {
        return jdbc.queryForObject("SELECT NOW(6)", LocalDateTime.class);
    }

    private LocalDateTime createdAt(long groupId) {
        return jdbc.queryForObject("SELECT created_at FROM social_groups WHERE id = ?", LocalDateTime.class, groupId);
    }

    @Test
    void existingGroups_GetTheLatestCreationTimeTheirPostsAndOrderAllow() {
        migrateTo("7");
        jdbc.update("""
                INSERT INTO users (login, password, first_name, email, phone_number)
                VALUES ('anna', 'hash', 'Anna', 'anna@example.com', '555-0100')
                """);
        long noPostsFirst = group();
        long postedAtTen = group();
        long noPostsBetween = group();
        long postedAtNine = group();
        long postedAtEleven = group();
        long noPostsLast = group();
        post(postedAtTen, TEN);
        post(postedAtTen, ELEVEN);
        // A later group can get its first post before an earlier one does.
        post(postedAtNine, NINE);
        post(postedAtEleven, ELEVEN);

        LocalDateTime beforeMigration = databaseNow();
        migrateTo("8");

        // Nine o'clock bounds every group up to the one first posted in at nine.
        assertEquals(NINE, createdAt(noPostsFirst));
        assertEquals(NINE, createdAt(postedAtTen));
        assertEquals(NINE, createdAt(noPostsBetween));
        assertEquals(NINE, createdAt(postedAtNine));
        assertEquals(ELEVEN, createdAt(postedAtEleven));
        // Nothing after it to go by: the time of the migration.
        assertTrue(!createdAt(noPostsLast).isBefore(beforeMigration), createdAt(noPostsLast).toString());
    }

    @Test
    void newGroups_AlwaysHaveACreationTime() {
        migrateTo("8");
        jdbc.update("""
                INSERT INTO users (login, password, first_name, email, phone_number)
                VALUES ('anna', 'hash', 'Anna', 'anna@example.com', '555-0100')
                """);
        LocalDateTime before = databaseNow();

        long id = group();

        // Inserted without one, the database stamps it.
        assertTrue(!createdAt(id).isBefore(before), createdAt(id).toString());
        String nullable = jdbc.queryForObject("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'social_groups' AND column_name = 'created_at'
                """, String.class);
        assertEquals("NO", nullable);
    }
}
