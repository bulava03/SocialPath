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
 * model derives the same lists with ordered queries; the id gives the order
 * of insertion.
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
     * Loads the newest direct replies of several posts or comments at once,
     * at most a given number per parent (a window function, MySQL 8). One
     * level of the tree per call, however many parents it has.
     * @param parentIds the posts or comments; must not be empty
     * @param perParent how many replies of each parent at most
     * @return the replies, newest first
     */
    @Query(value = """
            SELECT p.* FROM publications p
            JOIN (
                SELECT id, ROW_NUMBER() OVER (PARTITION BY parent_id ORDER BY id DESC) AS position
                FROM publications WHERE parent_id IN (:parentIds)
            ) ranked ON ranked.id = p.id
            WHERE ranked.position <= :perParent
            ORDER BY p.id DESC
            """, nativeQuery = true)
    List<Publication> findNewestReplies(@Param("parentIds") Collection<Long> parentIds,
                                        @Param("perParent") int perParent);

    /**
     * Loads a portion of the direct replies of one post or comment, newest
     * first, continuing by id like the feed itself.
     * @param parentId the post or comment
     * @param before only replies with a smaller id; null for the newest
     * @param limit how many replies at most
     * @return the replies, newest first
     */
    @Query("""
            select p from Publication p
            where p.parentId = :parentId and (:before is null or p.id < :before)
            order by p.id desc
            """)
    List<Publication> findReplies(@Param("parentId") Long parentId, @Param("before") Long before, Limit limit);

    /**
     * Counts the direct replies of several posts or comments at once.
     * @param parentIds the posts or comments; must not be empty
     * @return a count for each parent that has replies; parents without
     *         replies are left out
     */
    @Query("""
            select p.parentId as parentId, count(p) as total from Publication p
            where p.parentId in :parentIds
            group by p.parentId
            """)
    List<ReplyCount> countReplies(@Param("parentIds") Collection<Long> parentIds);

    /**
     * Counts the direct replies of a post or comment older than a given one.
     * @param parentId the post or comment
     * @param id the reply to count below
     * @return how many replies are left below it
     */
    long countByParentIdAndIdLessThan(Long parentId, Long id);

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
