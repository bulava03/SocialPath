package com.socialpath.repository;

import com.socialpath.entity.Publication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * Loads the direct children (comments) of a publication, oldest first.
     * @param parentId id of the parent publication or comment
     * @return the child publications in insertion order
     */
    List<Publication> findByParentIdOrderByCreatedAtAscIdAsc(Long parentId);

    /**
     * Finds the ids of a user's own page publications, oldest first.
     * @param login the page owner's login
     * @return publication ids in insertion order
     */
    @Query("""
            select p.id from Publication p
            where p.authorId = :login and p.parentId is null and p.groupId is null
            order by p.createdAt asc, p.id asc
            """)
    List<Long> findUserPublicationIds(@Param("login") String login);

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
     * Finds the ids of a group's publications, oldest first.
     * @param groupId the group id
     * @return publication ids in insertion order
     */
    @Query("""
            select p.id from Publication p
            where p.groupId = :groupId and p.parentId is null
            order by p.createdAt asc, p.id asc
            """)
    List<Long> findGroupPublicationIds(@Param("groupId") Long groupId);
}
