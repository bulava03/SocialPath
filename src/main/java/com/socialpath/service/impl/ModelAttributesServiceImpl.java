package com.socialpath.service.impl;

import lombok.RequiredArgsConstructor;
import com.socialpath.entity.User;
import com.socialpath.dto.request.*;
import com.socialpath.dto.response.*;
import com.socialpath.service.CommentsService;
import com.socialpath.service.CountryService;
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
    private final CountryService countryService;

    @Override
    public Model usersAttributes(Model model, User user, boolean isAuthor, FeedPage feed) {
        model.addAttribute("user", user);
        model.addAttribute("author", new UserLogin(user.getLogin(), ""));
        model.addAttribute("isAuthor", isAuthor);
        model.addAttribute("publications", feed.posts());
        model.addAttribute("nextBefore", feed.nextBefore());

        return model;
    }

    @Override
    public Model userInfoModel(Model model, User user, String login) {
        UserFormView userFormUpdate = modelMapper.map(user, UserFormView.class);
        fillDateOfBirth(userFormUpdate, user.getDateOfBirth());
        return userInfoModel(model, userFormUpdate, login);
    }

    @Override
    public Model userInfoModel(Model model, UserUpdate user, String login) {
        UserFormView userFormUpdate = modelMapper.map(user, UserFormView.class);
        // Echo the raw fields back, so an invalid date is shown as typed.
        userFormUpdate.setDay(user.getDay());
        userFormUpdate.setMonth(user.getMonth());
        userFormUpdate.setYear(user.getYear());
        return userInfoModel(model, userFormUpdate, login);
    }

    private Model userInfoModel(Model model, UserFormView form, String login) {
        model.addAttribute("user", form);
        model.addAttribute("author", login);
        model.addAttribute("countries", countryService.findAll());
        return model;
    }

    /**
     * Splits an optional date of birth into the day/month/year fields the
     * forms bind to. An absent date leaves them null, which the templates
     * render as empty placeholders.
     * @param form the form view being populated
     * @param dateOfBirth the stored date, possibly null
     */
    private void fillDateOfBirth(UserFormView form, LocalDateTime dateOfBirth) {
        if (dateOfBirth == null) {
            form.setDay(null);
            form.setMonth(null);
            form.setYear(null);
            return;
        }
        form.setDay(String.valueOf(dateOfBirth.getDayOfMonth()));
        form.setMonth(String.valueOf(dateOfBirth.getMonthValue()));
        form.setYear(String.valueOf(dateOfBirth.getYear()));
    }

    @Override
    public Model groupsAttributes(Model model, User author, List<?> admins, String groupId, String owner) {
        model.addAttribute("author", author);
        model.addAttribute("groupId", groupId);
        model.addAttribute("admins", admins);
        model.addAttribute("owner", owner);
        return model;
    }

}
