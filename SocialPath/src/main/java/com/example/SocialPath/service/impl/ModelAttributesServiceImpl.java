package com.example.SocialPath.service.impl;

import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.*;
import com.example.SocialPath.helper.ConvertHelper;
import com.example.SocialPath.service.CommentsService;
import com.example.SocialPath.service.ModelAttributesService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.List;

@Service
public class ModelAttributesServiceImpl implements ModelAttributesService {

    @Autowired
    private CommentsService commentsService;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public Model usersAttributes(Model model, User user, boolean isAuthor, List<PublicationPresentable> comments) {
        model.addAttribute("user", user);
        model.addAttribute("author", new UserLogin(user.getLogin(), user.getPassword()));
        model.addAttribute("isAuthor", isAuthor);
        model.addAttribute("publications", comments);

        return model;
    }

    @Override
    public Model userInfoModel(Model model, User user, UserLogin author) {
        UserFormUpdate userFormUpdate = modelMapper.map(user, UserFormUpdate.class);
        userFormUpdate.setDay(user.getDateOfBirth().getDayOfMonth());
        userFormUpdate.setMonth(ConvertHelper.monthToString(user.getDateOfBirth().getMonthValue()));
        userFormUpdate.setYear(user.getDateOfBirth().getYear());

        model.addAttribute("user", userFormUpdate);
        model.addAttribute("author", author);
        return model;
    }

    @Override
    public Model userInfoModel(Model model, UserUpdate user, UserLogin author) {
        UserFormUpdate userFormUpdate = modelMapper.map(user, UserFormUpdate.class);
        userFormUpdate.setDay(user.getDateOfBirth().getDayOfMonth());
        userFormUpdate.setMonth(ConvertHelper.monthToString(user.getDateOfBirth().getMonthValue()));
        userFormUpdate.setYear(user.getDateOfBirth().getYear());

        model.addAttribute("user", userFormUpdate);
        model.addAttribute("author", author);
        return model;
    }

    @Override
    public Model groupsAttributes(Model model, User author, List<String> admins, String groupId, String owner) {
        model.addAttribute("author", author);
        model.addAttribute("groupId", groupId);
        model.addAttribute("admins", admins);
        model.addAttribute("owner", owner);
        return model;
    }

    @Override
    public Model groupsAttributesFullList(Model model, User author, List<UserSearchResult> admins, String groupId, String owner) {
        model.addAttribute("author", author);
        model.addAttribute("groupId", groupId);
        model.addAttribute("admins", admins);
        model.addAttribute("owner", owner);
        return model;
    }

}
