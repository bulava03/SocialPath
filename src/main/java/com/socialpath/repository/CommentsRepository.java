package com.socialpath.repository;

import com.socialpath.entity.Publication;
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
     * Loads the posts of a user page, oldest first.
     * @param login the page owner's login
     * @return the top-level publications in insertion order
     */
    @Query("""
            select p from Publication p
            where p.authorId = :login and p.parentId is null and p.groupId is null
            order by p.createdAt asc, p.id asc
            """)
    List<Publication> findUserFeed(@Param("login") String login);

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
     * Loads the posts of a group page, oldest first.
     * @param groupId the group id
     * @return the top-level publications in insertion order
     */
    @Query("""
            select p from Publication p
            where p.groupId = :groupId and p.parentId is null
            order by p.createdAt asc, p.id asc
            """)
    List<Publication> findGroupFeed(@Param("groupId") Long groupId);

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
}
