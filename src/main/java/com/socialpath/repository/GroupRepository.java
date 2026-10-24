package com.socialpath.repository;

import com.socialpath.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    /**
     * Case-insensitive substring search over group names, the same matching
     * rule as the user search.
     * @param searchValue the substring to look for
     * @return groups whose name contains the substring
     */
    @Query("select g from Group g where lower(g.name) like lower(concat('%', :q, '%'))")
    List<Group> findByNameContaining(@Param("q") String searchValue);

    /**
     * Finds all groups the given login is a member of (replaces the
     * user-side groups array of the document model).
     * @param memberLogin the member's login
     * @return the groups the user belongs to
     */
    List<Group> findByMembersContains(String memberLogin);

    @Transactional
    @Modifying
    @Query("update Group g set g.name = :name, g.imageId = :imageId where g.id = :id")
    void updateGroup(@Param("id") Long id, @Param("name") String name, @Param("imageId") String imageId);

    @Transactional
    @Modifying
    @Query("update Group g set g.imageId = :imageId where g.id = :id")
    void updateImageId(@Param("id") Long id, @Param("imageId") String imageId);

    /*
     * Membership and adminship are changed with one statement on the join
     * table instead of loading the group, editing its list and saving it
     * back: no lost update when two people join at once, and the primary key
     * on (group_id, login) makes adding idempotent.
     */

    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO group_members (group_id, member_login) VALUES (:groupId, :login)
            ON DUPLICATE KEY UPDATE member_login = member_login
            """, nativeQuery = true)
    void addMember(@Param("groupId") Long groupId, @Param("login") String login);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM group_members WHERE group_id = :groupId AND member_login = :login",
            nativeQuery = true)
    void removeMember(@Param("groupId") Long groupId, @Param("login") String login);

    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO group_admins (group_id, admin_login) VALUES (:groupId, :login)
            ON DUPLICATE KEY UPDATE admin_login = admin_login
            """, nativeQuery = true)
    void addAdmin(@Param("groupId") Long groupId, @Param("login") String login);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM group_admins WHERE group_id = :groupId AND admin_login = :login",
            nativeQuery = true)
    void removeFromAdmins(@Param("groupId") Long groupId, @Param("login") String login);
}
