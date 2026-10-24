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

    default List<String> findMembersById(Long id) {
        Group group = findById(id).orElse(null);
        if (group != null) {
            return group.getMembers();
        }
        return null;
    }

    default List<String> findAdminsById(Long id) {
        Group group = findById(id).orElse(null);
        if (group != null) {
            return group.getAdmins();
        }
        return null;
    }

    default void addAdmin(Long groupId, String newAdminId) {
        Group group = findById(groupId).orElse(null);
        if (group != null && !group.getAdmins().contains(newAdminId)) {
            group.getAdmins().add(newAdminId);
            save(group);
        }
    }

    default void removeFromAdmins(Long groupId, String userId) {
        Group group = findById(groupId).orElse(null);
        if (group != null) {
            group.getAdmins().remove(userId);
            save(group);
        }
    }

    @Transactional
    @Modifying
    @Query("update Group g set g.name = :name, g.imageId = :imageId where g.id = :id")
    void updateGroup(@Param("id") Long id, @Param("name") String name, @Param("imageId") String imageId);

    @Transactional
    @Modifying
    @Query("update Group g set g.imageId = :imageId where g.id = :id")
    void updateImageId(@Param("id") Long id, @Param("imageId") String imageId);

    default void addMember(Long groupId, String memberId) {
        Group group = findById(groupId).orElse(null);
        if (group != null && !group.getMembers().contains(memberId)) {
            group.getMembers().add(memberId);
            save(group);
        }
    }

    default void removeMember(Long groupId, String memberId) {
        Group group = findById(groupId).orElse(null);
        if (group != null) {
            group.getMembers().remove(memberId);
            save(group);
        }
    }
}
