package com.socialpath.service.impl;

import lombok.RequiredArgsConstructor;
import com.socialpath.entity.User;
import com.socialpath.dto.request.UserUpdate;
import com.socialpath.dto.response.FeedPage;
import com.socialpath.service.CountryService;
import com.socialpath.service.ModelAttributesService;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelAttributesServiceImpl implements ModelAttributesService {

    private final CountryService countryService;

    @Override
    public Model usersAttributes(Model model, User user, boolean isAuthor, FeedPage feed) {
        model.addAttribute("user", user);
        model.addAttribute("author", user);
        model.addAttribute("isAuthor", isAuthor);
        model.addAttribute("publications", feed.posts());
        model.addAttribute("nextBefore", feed.nextBefore());

        return model;
    }

    @Override
    public Model userInfoModel(Model model, UserUpdate form) {
        model.addAttribute("user", form);
        model.addAttribute("countries", countryService.findAll());
        return model;
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
