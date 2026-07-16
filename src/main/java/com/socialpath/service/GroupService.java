package com.socialpath.service;

import com.socialpath.entity.Group;
import com.socialpath.dto.request.GroupCreationForm;
import com.socialpath.validation.ValidationResult;
import com.socialpath.dto.response.UserSearchResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Groups and their membership: creation, member and admin management, and
 * group profile updates. Authorization for these operations (who may remove
 * members or appoint admins) is enforced in the controller layer.
 */
public interface GroupService {

    /**
     * Validates a group creation form against its Bean Validation constraints.
     * @param group the form to validate
     * @return success, or a failure carrying the first violation message
     */
    ValidationResult validateGroup(GroupCreationForm group);

    /**
     * Persists a new group.
     * @param group the group to create
     * @return the stored group, including its generated id
     */
    Group addGroup(Group group);

    /**
     * Looks up a group by id.
     * @param id the group id
     * @return the group, or null if none exists
     */
    Group findGroupById(Long id);

    /**
     * Resolves a group's members into their presentable form.
     * @param id the group id
     * @return the members as search results
     */
    List<UserSearchResult> findGroupsMembers(Long id) throws IOException;

    /**
     * Returns the logins of a group's admins.
     * @param id the group id
     * @return the admin logins
     */
    List<String> findGroupsAdmins(Long id);

    /**
     * Resolves a group's admins into their presentable form.
     * @param id the group id
     * @return the admins as search results
     */
    List<UserSearchResult> findGroupsAdminsPresentable(Long id) throws IOException;

    /**
     * Revokes a user's admin rights in a group.
     * @param groupId the group id
     * @param userId the user to demote
     */
    void removeFromAdmins(Long groupId, String userId);

    /**
     * Grants a user admin rights in a group.
     * @param groupId the group id
     * @param userId the user to promote
     */
    void addToAdmins(Long groupId, String userId);

    /**
     * Removes a user from a group entirely, including any admin rights.
     * @param groupId the group id
     * @param userId the user to remove
     */
    void removeUserFromGroup(Long groupId, String userId);

    /**
     * Updates a group's name and avatar. A new file replaces the current
     * avatar, removeImage clears it without a replacement, otherwise the
     * avatar is left unchanged.
     * @param groupUpdated the group carrying the new name (and id)
     * @param file the uploaded avatar, possibly empty
     * @param removeImage whether to clear the current avatar
     */
    void updateGroup(Group groupUpdated, MultipartFile file, boolean removeImage) throws IOException;

    /**
     * Adds a member to a group.
     * @param groupId the group id, as a string
     * @param memberId the joining user's login
     */
    void addMember(String groupId, String memberId);

    /**
     * Removes a member from a group.
     * @param groupId the group id, as a string
     * @param memberId the leaving user's login
     */
    void removeMember(String groupId, String memberId);
}
