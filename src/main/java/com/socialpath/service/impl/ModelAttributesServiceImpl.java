package com.socialpath.service.impl;

import lombok.RequiredArgsConstructor;
import com.socialpath.document.User;
import com.socialpath.dto.request.*;
import com.socialpath.dto.response.*;
import com.socialpath.helper.ConvertHelper;
import com.socialpath.service.CommentsService;
import com.socialpath.service.ModelAttributesService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelAttributesServiceImpl implements ModelAttributesService {

    private final CommentsService commentsService;
    private final ModelMapper modelMapper;

    @Override
    public Model usersAttributes(Model model, User user, boolean isAuthor, List<PublicationPresentable> comments) {
        model.addAttribute("user", user);
        model.addAttribute("author", new UserLogin(user.getLogin(), ""));
        model.addAttribute("isAuthor", isAuthor);
        model.addAttribute("publications", comments);

        return model;
    }

    @Override
    public Model userInfoModel(Model model, User user, String login) {
        UserFormView userFormUpdate = modelMapper.map(user, UserFormView.class);
        fillDateOfBirth(userFormUpdate, user.getDateOfBirth());

        model.addAttribute("user", userFormUpdate);
        model.addAttribute("author", login);
        return model;
    }

    @Override
    public Model userInfoModel(Model model, UserUpdate user, String login) {
        UserFormView userFormUpdate = modelMapper.map(user, UserFormView.class);
        fillDateOfBirth(userFormUpdate, user.getDateOfBirth());

        model.addAttribute("user", userFormUpdate);
        model.addAttribute("author", login);
        return model;
    }

    /**
     * Splits an optional date of birth into the day/month/year fields the
     * forms bind to. An absent date renders as empty placeholders
     * (year == -1 is the "not set" convention the templates use).
     * @param form the form view being populated
     * @param dateOfBirth the stored date, possibly null
     */
    private void fillDateOfBirth(UserFormView form, LocalDateTime dateOfBirth) {
        if (dateOfBirth == null) {
            form.setDay(0);
            form.setMonth(null);
            form.setYear(-1);
            return;
        }
        form.setDay(dateOfBirth.getDayOfMonth());
        form.setMonth(ConvertHelper.monthToString(dateOfBirth.getMonthValue()));
        form.setYear(dateOfBirth.getYear());
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
