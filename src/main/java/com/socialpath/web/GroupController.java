package com.socialpath.web;

import com.socialpath.entity.Group;
import com.socialpath.entity.User;
import com.socialpath.dto.request.GroupCreationForm;
import com.socialpath.dto.request.GroupUserRequest;
import com.socialpath.dto.request.LeftFrameRequest;
import com.socialpath.dto.response.GroupSearchResult;
import com.socialpath.dto.response.UserLogin;
import com.socialpath.dto.response.UserSearchResult;
import com.socialpath.exception.ForbiddenOperationException;
import com.socialpath.exception.InvalidImageException;
import com.socialpath.validation.ValidationResult;
import com.socialpath.security.SecurityUtils;
import com.socialpath.service.GroupService;
import com.socialpath.service.HandleAvatarService;
import com.socialpath.service.ModelAttributesService;
import com.socialpath.service.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
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
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final ModelAttributesService modelAttributesService;
    private final HandleAvatarService handleAvatarService;

    @GetMapping("/getGroupCreationForm")
    public String getGroupCreationForm(Model model) {
        User myUser = userService.findByLogin(SecurityUtils.getCurrentLogin());

        model.addAttribute("author", new UserLogin(myUser.getLogin(), ""));
        model.addAttribute("group", new GroupCreationForm(myUser.getLogin()));
        return "group/groupCreationForm";
    }

    @PostMapping("/createGroup")
    public String createGroup(GroupCreationForm groupCreationForm, Model model) {
        String login = SecurityUtils.getCurrentLogin();
        User myUser = userService.findByLogin(login);

        ValidationResult validation = groupService.validateGroup(groupCreationForm);
        if (!validation.isSuccess()) {
            model.addAttribute("user", new UserLogin(myUser.getLogin(), ""));
            model.addAttribute("author", new UserLogin(myUser.getLogin(), ""));
            model.addAttribute("group", groupCreationForm);
            model.addAttribute("errorText", validation.getMessage());
            return "group/groupCreationForm";
        }

        Group group = modelMapper.map(groupCreationForm, Group.class);
        group.setOwner(login);
        group = groupService.addGroup(group);
        return "redirect:/searchResult/getGroupPage?groupId=" + group.getId();
    }

    @GetMapping("/getGroupsMembers")
    public String getGroupsMembers(LeftFrameRequest request, Model model) throws IOException {
        User user = userService.findByLogin(SecurityUtils.getCurrentLogin());
        Group group = groupService.getGroup(request.getId());

        List<UserSearchResult> list = groupService.findGroupsMembers(group.getId());
        List<String> admins = groupService.findGroupsAdmins(group.getId());

        model = modelAttributesService.groupsAttributes(model, user, admins, request.getId(), group.getOwner());
        model.addAttribute("users", list);
        return "group/groupsMembers";
    }

    @GetMapping("/getGroupsAdmins")
    public String getGroupsAdmins(LeftFrameRequest request, Model model) throws IOException {
        User user = userService.findByLogin(SecurityUtils.getCurrentLogin());
        Group group = groupService.getGroup(request.getId());

        List<UserSearchResult> list = groupService.findGroupsAdminsPresentable(group.getId());

        model = modelAttributesService.groupsAttributes(model, user, list, request.getId(), group.getOwner());
        return "group/groupsAdmins";
    }

    @PostMapping("/removeUserFromGroup")
    public String removeUserFromGroup(GroupUserRequest request) {
        String login = SecurityUtils.getCurrentLogin();
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
    public String addToAdmins(GroupUserRequest request) {
        String login = SecurityUtils.getCurrentLogin();
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
    public String removeFromAdmins(GroupUserRequest request) {
        demoteAdmin(request);
        return "redirect:/group/getGroupsMembers?id=" + request.getGroupId();
    }

    @PostMapping("/removeFromAdminsAdminsList")
    public String removeFromAdminsAdminsList(GroupUserRequest request) {
        demoteAdmin(request);
        return "redirect:/group/getGroupsAdmins?id=" + request.getGroupId();
    }

    @GetMapping("/getGroupUpdatingForm")
    public String getGroupUpdatingForm(LeftFrameRequest request, Model model) throws IOException {
        User user = userService.findByLogin(SecurityUtils.getCurrentLogin());
        Group groupFounded = groupService.getGroup(request.getId());

        if (!groupFounded.isOwner(user.getLogin())) {
            throw new ForbiddenOperationException("Only the group owner can edit the group");
        }

        return showGroupUpdatingForm(groupFounded, user, model);
    }

    @PostMapping("/updateGroup")
    public String updateGroup(Group group, String groupId, MultipartFile file,
                              @RequestParam(name = "removeImage", defaultValue = "false") boolean removeImage,
                              Model model)
            throws IOException {
        User user = userService.findByLogin(SecurityUtils.getCurrentLogin());
        Group groupFounded = groupService.getGroup(groupId);

        if (!groupFounded.isOwner(user.getLogin())) {
            throw new ForbiddenOperationException("Only the group owner can edit the group");
        }

        group.setId(groupFounded.getId());
        try {
            groupService.updateGroup(group, file, removeImage);
        } catch (InvalidImageException e) {
            model.addAttribute("errorText", e.getMessage());
            return showGroupUpdatingForm(groupFounded, user, model);
        }

        return "redirect:/searchResult/getGroupPage?groupId=" + group.getId();
    }

    private String showGroupUpdatingForm(Group groupFounded, User user, Model model) {
        GroupSearchResult group = GroupSearchResult.from(groupFounded);
        group = handleAvatarService.resolveDisplayAvatar(groupFounded, group);

        model.addAttribute("group", group);
        model.addAttribute("author", user);
        model = handleAvatarService.handleAvatar(user, model, false);

        return "group/groupUpdatingForm";
    }

    private void demoteAdmin(GroupUserRequest request) {
        String login = SecurityUtils.getCurrentLogin();
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
