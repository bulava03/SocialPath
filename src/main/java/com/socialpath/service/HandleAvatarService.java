package com.socialpath.service;

import com.socialpath.entity.Group;
import com.socialpath.entity.User;
import com.socialpath.dto.response.GroupSearchResult;
import com.socialpath.dto.request.UserUpdate;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Owns all avatar handling: placing avatar image ids into the model for
 * rendering (templates turn them into /images/{id} URLs) and applying avatar
 * changes on profile and group updates. Keeping both the user and group avatar
 * logic here means file cleanup lives in one place rather than split across
 * services.
 */
public interface HandleAvatarService {

    /**
     * Adds a user's avatar image id to the model.
     * @param user the user whose avatar is shown
     * @param model the model to populate
     * @param isAnother true when this is another user's avatar (uses a distinct
     *                  attribute so a page can show both viewer and subject)
     * @return the same model, for chaining
     */
    Model handleAvatar(User user, Model model, boolean isAnother);

    /**
     * Adds a group's avatar image id to the model.
     * @param group the group whose avatar is shown
     * @param model the model to populate
     * @return the same model, for chaining
     */
    Model handleAvatar(Group group, Model model);

    /**
     * Applies an avatar change to a profile update: a new upload replaces (and
     * deletes) the previous image, removeImage clears it without a replacement,
     * otherwise the current avatar is kept.
     * @param user the profile update being assembled
     * @param myUser the persisted user, source of the current image id
     * @param file the uploaded avatar, possibly empty
     * @return the update with its image id set to the result
     */
    UserUpdate updateAvatar(UserUpdate user, User myUser, MultipartFile file) throws IOException;

    /**
     * Applies an avatar change to a group: a new upload replaces the old image,
     * removeImage deletes it without a replacement, otherwise it is kept.
     * @param group the group whose stored image id is the current state
     * @param file the uploaded image, possibly empty
     * @param removeImage whether the user asked to clear the current image
     * @return the resulting image id ("" when cleared, unchanged otherwise)
     */
    String updateGroupAvatar(Group group, MultipartFile file, boolean removeImage) throws IOException;

    /**
     * Normalizes a group's stored avatar id for display (empty becomes null so
     * the template falls back to the default image).
     * @param group the group whose stored image id is read
     * @param result the view model to populate
     * @return the same view model, for chaining
     */
    GroupSearchResult resolveDisplayAvatar(Group group, GroupSearchResult result);
}
