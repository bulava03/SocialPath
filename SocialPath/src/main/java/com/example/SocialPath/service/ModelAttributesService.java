package com.example.SocialPath.service;

import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.PublicationPresentable;
import com.example.SocialPath.extraClasses.UserLogin;
import com.example.SocialPath.extraClasses.UserSearchResult;
import com.example.SocialPath.extraClasses.UserUpdate;
import org.bson.types.ObjectId;
import org.springframework.ui.Model;

import java.util.List;

public interface ModelAttributesService {
    Model usersAttributes(Model model, User user, boolean isAuthor, List<PublicationPresentable> comments);

    Model userInfoModel(Model model, User user, UserLogin author);

    Model userInfoModel(Model model, UserUpdate user, UserLogin author);

    Model groupsAttributes(Model model, User author, List<String> admins, String groupId, String owner);

    Model groupsAttributesFullList(Model model, User author, List<UserSearchResult> admins, String groupId, String owner);
}
