package com.socialpath.service.impl;

import com.socialpath.entity.Group;
import com.socialpath.entity.User;
import com.socialpath.dto.request.UserUpdate;
import com.socialpath.dto.response.GroupSearchResult;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.HandleAvatarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Puts avatar image ids into the model (templates turn them into
 * /images/{id} URLs) and handles avatar replacement on profile updates.
 */
@Service
@RequiredArgsConstructor
public class HandleAvatarServiceImpl implements HandleAvatarService {

    private final FileStorageService fileStorageService;

    @Override
    public Model handleAvatar(User user, Model model, boolean isAnother) {
        model.addAttribute(isAnother ? "avatarAnother" : "avatar", normalize(user.getImageId()));
        return model;
    }

    @Override
    public Model handleAvatar(Group group, Model model) {
        model.addAttribute("groupAvatar", normalize(group.getImageId()));
        return model;
    }

    /**
     * Applies an avatar change: a newly uploaded file replaces (and deletes)
     * the previous one; the removeImage flag deletes the avatar without a
     * replacement; otherwise the current avatar is kept untouched.
     * @param user the profile update being assembled
     * @param myUser the persisted user, source of the current image id
     * @param file the uploaded avatar, possibly empty
     * @return the update with the resulting image id set
     */
    @Override
    public UserUpdate updateAvatar(UserUpdate user, User myUser, MultipartFile file) throws IOException {
        boolean hasStoredImage = myUser.getImageId() != null && !myUser.getImageId().isEmpty();

        if (file != null && !file.isEmpty()) {
            if (hasStoredImage) {
                fileStorageService.deleteFileById(myUser.getImageId());
            }
            user.setImageId(fileStorageService.storeFile(file));
        } else if (user.isRemoveImage()) {
            if (hasStoredImage) {
                fileStorageService.deleteFileById(myUser.getImageId());
            }
            user.setImageId("");
        } else {
            user.setImageId(myUser.getImageId());
        }
        return user;
    }

    @Override
    public String updateGroupAvatar(Group group, MultipartFile file, boolean removeImage) throws IOException {
        boolean hasStoredImage = group.getImageId() != null && !group.getImageId().isEmpty();

        if (file != null && !file.isEmpty()) {
            if (hasStoredImage) {
                fileStorageService.deleteFileById(group.getImageId());
            }
            return fileStorageService.storeFile(file);
        }
        if (removeImage && hasStoredImage) {
            fileStorageService.deleteFileById(group.getImageId());
            return "";
        }
        return group.getImageId();
    }

    @Override
    public GroupSearchResult resolveDisplayAvatar(Group group, GroupSearchResult result) {
        result.setImageId(normalize(group.getImageId()));
        return result;
    }

    private String normalize(String imageId) {
        return (imageId == null || imageId.isEmpty()) ? null : imageId;
    }
}
