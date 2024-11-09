package com.example.SocialPath.service.impl;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.GroupSearchResult;
import com.example.SocialPath.extraClasses.UserUpdate;
import com.example.SocialPath.service.HandleAvatarService;
import com.example.SocialPath.service.FileStorageService;
import com.example.SocialPath.service.GroupService;
import com.example.SocialPath.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@Service
public class HandleAvatarServiceImpl implements HandleAvatarService {

    @Autowired
    private UserService userService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private FileStorageService fileStorageService;

    private String getAvatarAttr(boolean isAnother) {
        if (isAnother) {
            return "avatarAnother";
        } else {
            return "avatar";
        }
    }

    public Model handleAvatar(User user, Model model, boolean isAnother) throws IOException {
        String avatarAttr = getAvatarAttr(isAnother);

        if (user.getImageId() != null && !user.getImageId().isEmpty()) {
            GridFsResource file = fileStorageService.getFileById(user.getImageId());
            model.addAttribute(avatarAttr, fileStorageService.convertGridFsFileToBase64(file));
        } else {
            model.addAttribute(avatarAttr, null);
        }

        return model;
    }

    public Model handleAvatar(Group group, Model model) throws IOException {
        if (group.getImageId() != null && !group.getImageId().isEmpty()) {
            GridFsResource file = fileStorageService.getFileById(group.getImageId());
            model.addAttribute("groupAvatar", fileStorageService.convertGridFsFileToBase64(file));
        } else {
            model.addAttribute("groupAvatar", null);
        }

        return model;
    }

    public UserUpdate updateAvatar(UserUpdate user, User myUser, MultipartFile file) throws IOException {
        if (!file.isEmpty()) {
            if (!Objects.equals(myUser.getImageId(), "") && (myUser.getImageId() != null)) {
                fileStorageService.deleteFileById(myUser.getImageId());
                user.setImageId("");
            }
            String fileId = fileStorageService.storeFile(file);
            user.setImageId(fileId);
        } else {
            if (!Objects.equals(user.getImageId(), "") && (user.getImageId() != null)) {
                fileStorageService.deleteFileById(myUser.getImageId());
                user.setImageId("");
            } else {
                user.setImageId("");
            }
        }

        return user;
    }

    public GroupSearchResult updateAvatar(Group group, GroupSearchResult result) throws IOException {
        String file;
        if (group.getImageId() == null || group.getImageId().isEmpty()) {
            file = null;
        } else {
            GridFsResource resource = fileStorageService.getFileById(group.getImageId());
            file = fileStorageService.convertGridFsFileToBase64(resource);
        }
        result.setFile(file);

        return result;
    }

}
