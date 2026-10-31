package com.socialpath.service;

import com.socialpath.entity.User;
import com.socialpath.dto.response.FeedPage;
import com.socialpath.dto.request.UserUpdate;
import org.springframework.ui.Model;

import java.util.List;

/**
 * Populates Thymeleaf models with the recurring attribute sets that the user
 * and group pages need, so controllers don't repeat the wiring.
 */
public interface ModelAttributesService {

    /**
     * Adds the attributes a user page needs: the user, authorship flag and feed.
     * @param model the model to populate
     * @param user the page owner
     * @param isAuthor whether the viewer owns this page
     * @param feed the first portion of the page's feed
     * @return the same model, for chaining
     */
    Model usersAttributes(Model model, User user, boolean isAuthor, FeedPage feed);

    /**
     * Adds what the profile-info form needs: the form itself and the countries.
     * @param model the model to populate
     * @param form the form, filled from the stored user or as submitted
     * @return the same model, for chaining
     */
    Model userInfoModel(Model model, UserUpdate form);

    /**
     * Adds the attributes the group members and admins pages need.
     * @param model the model to populate
     * @param author the viewer
     * @param admins the group's admins: logins on the members page, full
     *               list items on the admins page
     * @param groupId the group id
     * @param owner the group owner's login
     * @return the same model, for chaining
     */
    Model groupsAttributes(Model model, User author, List<?> admins, String groupId, String owner);
}
