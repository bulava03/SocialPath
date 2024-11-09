package com.example.SocialPath.service;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.GroupSearchResult;
import com.example.SocialPath.extraClasses.UserUpdate;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface HandleAvatarService {
    Model handleAvatar(User user, Model model, boolean isAnother) throws IOException;
    Model handleAvatar(Group group, Model model) throws IOException;
    UserUpdate updateAvatar(UserUpdate user, User myUser, MultipartFile file) throws IOException;
    GroupSearchResult updateAvatar(Group group, GroupSearchResult result) throws IOException;
}
