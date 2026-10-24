package com.socialpath.repository;

import com.socialpath.entity.Publication;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repository for publications and comments (one table, self-referencing
 * parent_id). Where the document model kept arrays of child ids, the SQL
 * model derives the same lists with ordered queries; insertion order is
 * reproduced by sorting on (created_at, id).
 */
@Repository
public interface CommentsRepository extends JpaRepository<Publication, Long> {

    /**
     * Loads a portion of a user page's posts, newest first. Continuing from
     * an id instead of an offset keeps portions stable while new posts come
     * in: they are newer, so they never shift the older portions.
     * @param login the page owner's login
     * @param before only posts with a smaller id; null for the newest
     * @param limit how many posts at most
     * @return the top-level publications, newest first
     */
    @Query("""
            select p from Publication p
            where p.authorId = :login and p.parentId is null and p.groupId is null
              and (:before is null or p.id < :before)
            order by p.id desc
            """)
    List<Publication> findUserFeed(@Param("login") String login, @Param("before") Long before, Limit limit);

    /**
     * Collects the media file ids of a publication and of every comment in
     * its subtree in one round trip, using a recursive CTE (MySQL 8). Used
     * before deletion: the rows are removed by the database's ON DELETE
     * CASCADE, but the files live outside the database and must be cleaned
     * up by the application.
     * @param rootId id of the subtree root
     * @return media file ids of the whole subtree
     */
    @Query(value = """
            WITH RECURSIVE tree AS (
                SELECT id FROM publications WHERE id = :rootId
                UNION ALL
                SELECT p.id FROM publications p JOIN tree t ON p.parent_id = t.id
            )
            SELECT pm.file_id FROM publication_media pm
            JOIN tree t ON pm.publication_id = t.id
            """, nativeQuery = true)
    List<String> findSubtreeMediaFileIds(@Param("rootId") Long rootId);

    /**
     * Loads a portion of a group page's posts, newest first.
     * @param groupId the group id
     * @param before only posts with a smaller id; null for the newest
     * @param limit how many posts at most
     * @return the top-level publications, newest first
     */
    @Query("""
            select p from Publication p
            where p.groupId = :groupId and p.parentId is null
              and (:before is null or p.id < :before)
            order by p.id desc
            """)
    List<Publication> findGroupFeed(@Param("groupId") Long groupId, @Param("before") Long before, Limit limit);

    /**
     * Loads every comment under the given posts, at any depth, in one round
     * trip (recursive CTE, MySQL 8). The caller builds the tree from
     * parent_id; rows come oldest first, so siblings keep insertion order.
     * @param rootIds ids of the top-level publications; must not be empty
     * @return all comments and replies below them
     */
    @Query(value = """
            WITH RECURSIVE tree AS (
                SELECT * FROM publications WHERE parent_id IN (:rootIds)
                UNION ALL
                SELECT p.* FROM publications p JOIN tree t ON p.parent_id = t.id
            )
            SELECT * FROM tree ORDER BY created_at, id
            """, nativeQuery = true)
    List<Publication> findReplies(@Param("rootIds") Collection<Long> rootIds);

    /**
     * The newest post or comment anywhere in a user page's feed that someone
     * other than the viewer wrote. Lets an open page notice new activity
     * without the viewer's own posts and comments counting as news.
     * @param owner the page owner's login
     * @param viewer the viewer's login
     * @return the largest such id, or null when there is none
     */
    @Query(value = """
            WITH RECURSIVE tree AS (
                SELECT id, author_login FROM publications
                WHERE author_login = :owner AND parent_id IS NULL AND group_id IS NULL
                UNION ALL
                SELECT p.id, p.author_login FROM publications p JOIN tree t ON p.parent_id = t.id
            )
            SELECT MAX(id) FROM tree WHERE author_login <> :viewer
            """, nativeQuery = true)
    Long findLatestUserFeedActivity(@Param("owner") String owner, @Param("viewer") String viewer);

    /**
     * The newest post or comment anywhere in a group's feed that someone
     * other than the viewer wrote.
     * @param groupId the group id
     * @param viewer the viewer's login
     * @return the largest such id, or null when there is none
     */
    @Query(value = """
            WITH RECURSIVE tree AS (
                SELECT id, author_login FROM publications
                WHERE group_id = :groupId AND parent_id IS NULL
                UNION ALL
                SELECT p.id, p.author_login FROM publications p JOIN tree t ON p.parent_id = t.id
            )
            SELECT MAX(id) FROM tree WHERE author_login <> :viewer
            """, nativeQuery = true)
    Long findLatestGroupFeedActivity(@Param("groupId") Long groupId, @Param("viewer") String viewer);
}
