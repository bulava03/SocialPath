package com.socialpath.service.impl;

import com.socialpath.entity.Group;
import com.socialpath.entity.User;
import com.socialpath.dto.request.UserUpdate;
import com.socialpath.dto.response.GroupSearchResult;
import com.socialpath.media.AvatarImageProcessor;
import com.socialpath.media.AvatarImageProcessor.ProcessedAvatar;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.HandleAvatarService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Puts avatar image ids into the model (templates turn them into
 * /images/{id} URLs) and handles avatar replacement on profile updates.
 * Every stored avatar is processed (see {@link AvatarImageProcessor}) into a
 * full version "{uuid}.jpg" and a thumbnail "{uuid}_thumb.jpg".
 */
@Service
@RequiredArgsConstructor
public class HandleAvatarServiceImpl implements HandleAvatarService {

    private final FileStorageService fileStorageService;
    private final AvatarImageProcessor avatarImageProcessor;

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
     * Applies an avatar change: a newly uploaded file replaces the previous
     * one; the removeImage flag deletes the avatar without a replacement;
     * otherwise the current avatar is kept untouched. The new file is
     * processed before the old one is deleted, so a rejected upload leaves the
     * current avatar in place.
     * @param user the profile update being assembled
     * @param myUser the persisted user, source of the current image id
     * @param file the uploaded avatar, possibly empty
     * @return the update with the resulting image id set
     */
    @Override
    public UserUpdate updateAvatar(UserUpdate user, User myUser, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            user.setImageId(storeAvatar(file));
            deleteAvatar(myUser.getImageId());
        } else if (user.isRemoveImage()) {
            deleteAvatar(myUser.getImageId());
            user.setImageId("");
        } else {
            user.setImageId(myUser.getImageId());
        }
        return user;
    }

    @Override
    public String updateGroupAvatar(Group group, MultipartFile file, boolean removeImage) throws IOException {
        if (file != null && !file.isEmpty()) {
            String newImageId = storeAvatar(file);
            deleteAvatar(group.getImageId());
            return newImageId;
        }
        if (removeImage && normalize(group.getImageId()) != null) {
            deleteAvatar(group.getImageId());
            return "";
        }
        return group.getImageId();
    }

    @Override
    public GroupSearchResult resolveDisplayAvatar(Group group, GroupSearchResult result) {
        result.setImageId(normalize(group.getImageId()));
        return result;
    }

    @Override
    public boolean needsProcessing(String imageId) {
        return normalize(imageId) != null
                && fileStorageService.getFileById(HandleAvatarService.thumbnailIdOf(imageId)) == null;
    }

    @Override
    public String reprocessStoredAvatar(String imageId) throws IOException {
        Resource stored = fileStorageService.getFileById(imageId);
        if (stored == null) {
            throw new IOException("Avatar file is missing: " + imageId);
        }
        return storeAvatar(stored);
    }

    @Override
    public void deleteAvatar(String imageId) {
        if (normalize(imageId) == null) {
            return;
        }
        fileStorageService.deleteFileById(imageId);
        fileStorageService.deleteFileById(HandleAvatarService.thumbnailIdOf(imageId));
    }

    /**
     * Processes a picture and stores both versions under a new id. The
     * thumbnail is written first: a stored full version therefore always has
     * its thumbnail, which is what needsProcessing() relies on.
     * @param source the picture (an upload or a stored file)
     * @return the new avatar id
     */
    private String storeAvatar(InputStreamSource source) throws IOException {
        ProcessedAvatar processed = avatarImageProcessor.process(source);
        String imageId = UUID.randomUUID() + ".jpg";
        fileStorageService.storeFile(HandleAvatarService.thumbnailIdOf(imageId), processed.thumbnail());
        fileStorageService.storeFile(imageId, processed.full());
        return imageId;
    }

    private String normalize(String imageId) {
        return (imageId == null || imageId.isEmpty()) ? null : imageId;
    }
}
