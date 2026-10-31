package com.socialpath.web;

import com.socialpath.entity.Group;
import com.socialpath.entity.GroupCategory;
import com.socialpath.entity.User;
import com.socialpath.dto.request.GroupForm;
import com.socialpath.dto.request.GroupUserRequest;
import com.socialpath.dto.request.LeftFrameRequest;
import com.socialpath.dto.response.GroupSearchResult;
import com.socialpath.dto.response.UserSearchResult;
import com.socialpath.exception.ForbiddenOperationException;
import com.socialpath.exception.InvalidImageException;
import com.socialpath.validation.ValidationResult;
import com.socialpath.security.CurrentUser;
import com.socialpath.service.GroupService;
import com.socialpath.service.HandleAvatarService;
import com.socialpath.service.ModelAttributesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final ModelAttributesService modelAttributesService;
    private final HandleAvatarService handleAvatarService;

    @GetMapping("/getGroupCreationForm")
    public String getGroupCreationForm(@CurrentUser User myUser, Model model) {
        return showGroupCreationForm(myUser, new GroupForm(), null, model);
    }

    @PostMapping("/createGroup")
    public String createGroup(@CurrentUser User myUser, GroupForm form, Model model) {
        ValidationResult validation = groupService.validateGroup(form);
        if (!validation.isSuccess()) {
            return showGroupCreationForm(myUser, form, validation.getMessage(), model);
        }

        Group group = new Group();
        form.applyTo(group);
        group.setOwner(myUser.getLogin());
        group = groupService.addGroup(group);
        return "redirect:/searchResult/getGroupPage?groupId=" + group.getId();
    }

    private String showGroupCreationForm(User user, GroupForm form, String errorText, Model model) {
        model.addAttribute("author", user);
        model.addAttribute("group", form);
        model.addAttribute("categories", GroupCategory.values());
        if (errorText != null) {
            // Only when there is one: an upload over the limit brings its message as a flash attribute.
            model.addAttribute("errorText", errorText);
        }
        handleAvatarService.handleAvatar(user, model, false);
        return "group/groupCreationForm";
    }

    @GetMapping("/getGroupsMembers")
    public String getGroupsMembers(@CurrentUser User user, LeftFrameRequest request, Model model) {
        Group group = groupService.getGroup(request.getId());

        List<UserSearchResult> list = groupService.findGroupsMembers(group.getId());
        List<String> admins = groupService.findGroupsAdmins(group.getId());

        model = modelAttributesService.groupsAttributes(model, user, admins, request.getId(), group.getOwner());
        model.addAttribute("users", list);
        handleAvatarService.handleAvatar(user, model, false);
        return "group/groupsMembers";
    }

    @GetMapping("/getGroupsAdmins")
    public String getGroupsAdmins(@CurrentUser User user, LeftFrameRequest request, Model model) {
        Group group = groupService.getGroup(request.getId());

        List<UserSearchResult> list = groupService.findGroupsAdminsPresentable(group.getId());

        model = modelAttributesService.groupsAttributes(model, user, list, request.getId(), group.getOwner());
        handleAvatarService.handleAvatar(user, model, false);
        return "group/groupsAdmins";
    }

    @PostMapping("/removeUserFromGroup")
    public String removeUserFromGroup(@CurrentUser User me, GroupUserRequest request) {
        String login = me.getLogin();
        Group group = groupService.getGroup(request.getGroupId());

        boolean actorIsOwner = group.isOwner(login);
        boolean actorIsAdmin = group.isAdmin(login);
        boolean targetIsAdmin = group.isAdmin(request.getUserId()) || group.isOwner(request.getUserId());

        // The owner can remove anyone but themselves; admins can remove plain members.
        if (group.isOwner(request.getUserId())
                || (!actorIsOwner && !actorIsAdmin)
                || (!actorIsOwner && targetIsAdmin)) {
            throw new ForbiddenOperationException("Not allowed to remove this member");
        }

        groupService.removeUserFromGroup(group.getId(), request.getUserId());

        return "redirect:/group/getGroupsMembers?id=" + request.getGroupId();
    }

    @PostMapping("/addToAdmins")
    public String addToAdmins(@CurrentUser User me, GroupUserRequest request) {
        String login = me.getLogin();
        Group group = groupService.getGroup(request.getGroupId());

        if (!group.isOwner(login)) {
            throw new ForbiddenOperationException("Only the group owner can appoint admins");
        }
        if (!group.isMember(request.getUserId())) {
            throw new ForbiddenOperationException("Only group members can become admins");
        }

        groupService.addToAdmins(group.getId(), request.getUserId());

        return "redirect:/group/getGroupsMembers?id=" + request.getGroupId();
    }

    @PostMapping("/removeFromAdmins")
    public String removeFromAdmins(@CurrentUser User me, GroupUserRequest request) {
        demoteAdmin(me, request);
        return "redirect:/group/getGroupsMembers?id=" + request.getGroupId();
    }

    @PostMapping("/removeFromAdminsAdminsList")
    public String removeFromAdminsAdminsList(@CurrentUser User me, GroupUserRequest request) {
        demoteAdmin(me, request);
        return "redirect:/group/getGroupsAdmins?id=" + request.getGroupId();
    }

    @GetMapping("/getGroupUpdatingForm")
    public String getGroupUpdatingForm(@CurrentUser User user, LeftFrameRequest request, Model model) {
        Group groupFounded = groupService.getGroup(request.getId());

        if (!groupFounded.isOwner(user.getLogin())) {
            throw new ForbiddenOperationException("Only the group owner can edit the group");
        }

        return showGroupUpdatingForm(groupFounded, GroupForm.from(groupFounded), user, null, model);
    }

    @PostMapping("/updateGroup")
    public String updateGroup(@CurrentUser User user, GroupForm form, String groupId, MultipartFile file,
                              @RequestParam(name = "removeImage", defaultValue = "false") boolean removeImage,
                              Model model)
            throws IOException {
        Group groupFounded = groupService.getGroup(groupId);

        if (!groupFounded.isOwner(user.getLogin())) {
            throw new ForbiddenOperationException("Only the group owner can edit the group");
        }

        ValidationResult validation = groupService.validateGroup(form);
        if (!validation.isSuccess()) {
            return showGroupUpdatingForm(groupFounded, form, user, validation.getMessage(), model);
        }
        try {
            groupService.updateGroup(groupFounded.getId(), form, file, removeImage);
        } catch (InvalidImageException e) {
            return showGroupUpdatingForm(groupFounded, form, user, e.getMessage(), model);
        }

        return "redirect:/searchResult/getGroupPage?groupId=" + groupFounded.getId();
    }

    // The group gives the id and the avatar, the form the values shown in the fields.
    private String showGroupUpdatingForm(Group groupFounded, GroupForm form, User user, String errorText, Model model) {
        GroupSearchResult group = GroupSearchResult.from(groupFounded);
        group = handleAvatarService.resolveDisplayAvatar(groupFounded, group);

        model.addAttribute("group", group);
        model.addAttribute("form", form);
        model.addAttribute("categories", GroupCategory.values());
        if (errorText != null) {
            // Only when there is one: an upload over the limit brings its message as a flash attribute.
            model.addAttribute("errorText", errorText);
        }
        model.addAttribute("author", user);
        model = handleAvatarService.handleAvatar(user, model, false);

        return "group/groupUpdatingForm";
    }

    private void demoteAdmin(User me, GroupUserRequest request) {
        String login = me.getLogin();
        Group group = groupService.getGroup(request.getGroupId());

        if (!group.isOwner(login)) {
            throw new ForbiddenOperationException("Only the group owner can demote admins");
        }
        if (group.isOwner(request.getUserId())) {
            throw new ForbiddenOperationException("The group owner cannot be demoted");
        }

        groupService.removeFromAdmins(group.getId(), request.getUserId());
    }
}
