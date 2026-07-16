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
    List<Group> findByNameIn(List<String> searchValues);

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

    default void removeFromGroup(Long groupId, String userId) {
        Group group = findById(groupId).orElse(null);
        if (group != null) {
            group.getMembers().remove(userId);
            save(group);
        }
    }

    @Transactional
    @Modifying
    @Query("update Group g set g.name = :name, g.imageId = :imageId where g.id = :id")
    void updateGroup(@Param("id") Long id, @Param("name") String name, @Param("imageId") String imageId);

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
