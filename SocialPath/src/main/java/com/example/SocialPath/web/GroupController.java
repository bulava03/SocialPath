package com.example.SocialPath.web;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.*;
import com.example.SocialPath.helper.CheckHelper;
import com.example.SocialPath.service.*;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/group")
public class GroupController {

    @Autowired
    private GroupService groupService;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentsService commentsService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ModelAttributesService modelAttributesService;
    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/getGroupCreationForm")
    public String getGroupCreationForm(@ModelAttribute("author") UserLogin author, Model model) {
        model.addAttribute("author", author);
        model.addAttribute("group", new GroupCreationForm(author.getLogin(), author.getPassword()));
        return "group/groupCreationForm";
    }

    @PostMapping("/createGroup")
    public String createGroup(@ModelAttribute("group") GroupCreationForm groupCreationForm, Model model) {
        Object[] validation = groupService.validateGroup(groupCreationForm);
        if (!(boolean) validation[0]) {
            model.addAttribute("user", new UserLogin(groupCreationForm.getOwner(), groupCreationForm.getOwnerPassword()));
            model.addAttribute("author", new UserLogin(groupCreationForm.getOwner(), groupCreationForm.getOwnerPassword()));
            model.addAttribute("group", groupCreationForm);
            model.addAttribute("errorText", validation[1].toString().replaceAll("Optional\\[|\\]", ""));
            return "group/groupCreationForm";
        }
        if (userService.findUserByLoginAndPassword(groupCreationForm.getOwner(), groupCreationForm.getOwnerPassword()) != null) {
            Group group = modelMapper.map(groupCreationForm, Group.class);
            group = groupService.addGroup(group);
            return "redirect:/searchResult/getGroupPage?login=" + groupCreationForm.getOwner() +
                    "&password=" + groupCreationForm.getOwnerPassword() + "&groupId=" + group.getId();
        }
        return "redirect:/user/authorisation?login=" + groupCreationForm.getOwner() + "&password=" + groupCreationForm.getOwnerPassword();
    }

    @GetMapping("/getGroupsMembers")
    public String getGroupsMembers(@ModelAttribute("request") LeftFrameRequest request, Model model) throws IOException {
        User user = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        List<UserSearchResult> list = groupService.findGroupsMembers(new ObjectId(request.getId()));
        List<String> admins = groupService.findGroupsAdmins(new ObjectId(request.getId()));
        String owner = groupService.findGroupOwner(new ObjectId(request.getId()));

        if (groupService.findGroupById(new ObjectId(request.getId())) == null) {
            model = modelAttributesService.usersAttributes(model, user, true, commentsService.loadComments("User", user.getLogin()));
            return "user/userPage";
        }

        model = modelAttributesService.groupsAttributes(model, user, admins, request.getId(), owner);
        model.addAttribute("users", list);
        return "group/groupsMembers";
    }

    @GetMapping("/getGroupsAdmins")
    public String getGroupsAdmins(@ModelAttribute("request") LeftFrameRequest request, Model model) throws IOException {
        User user = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        List<UserSearchResult> list = groupService.findGroupsAdminsPresentable(new ObjectId(request.getId()));
        String owner = groupService.findGroupOwner(new ObjectId(request.getId()));

        if (groupService.findGroupById(new ObjectId(request.getId())) == null) {
            model = modelAttributesService.usersAttributes(model, user, true, commentsService.loadComments("User", user.getLogin()));
            return "user/userPage";
        }

        model = modelAttributesService.groupsAttributesFullList(model, user, list, request.getId(), owner);
        return "group/groupsAdmins";
    }

    @PostMapping("/joinGroup")
    public String joinGroup(@ModelAttribute("request") LeftFrameRequest request, Model model) {
        User user = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        groupService.joinGroup(new ObjectId(request.getId()), request.getAuthorLogin());

        model.addAttribute("group", groupService.findGroupById(new ObjectId(request.getId())));
        model.addAttribute("author", new UserLogin(request.getAuthorLogin(), request.getAuthorPassword()));
        model.addAttribute("publications", commentsService.loadComments("Group", request.getId()));
        return "group/groupPage";
    }

    @PostMapping("/leaveGroup")
    public String leaveGroup(@ModelAttribute("request") LeftFrameRequest request, Model model) {
        User user = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        groupService.removeFromAdmins(new ObjectId(request.getId()), request.getAuthorLogin());
        groupService.removeFromGroup(new ObjectId(request.getId()), request.getAuthorLogin());

        model.addAttribute("group", groupService.findGroupById(new ObjectId(request.getId())));
        model.addAttribute("author", new UserLogin(request.getAuthorLogin(), request.getAuthorPassword()));
        model.addAttribute("publications", commentsService.loadComments("Group", request.getId()));
        return "group/groupPage";
    }

    @PostMapping("/removeUserFromGroup")
    public String removeUserFromGroup(@ModelAttribute("request") GroupUserRequest request, Model model) throws IOException {
        User myUser = userService.findUserByLoginAndPassword(request.getLogin(), request.getPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        }

        groupService.removeUserFromGroup(new ObjectId(request.getGroupId()), request.getUserId());

        List<UserSearchResult> list = groupService.findGroupsMembers(new ObjectId(request.getGroupId()));
        List<String> admins = groupService.findGroupsAdmins(new ObjectId(request.getGroupId()));
        String owner = groupService.findGroupOwner(new ObjectId(request.getGroupId()));

        if (groupService.findGroupById(new ObjectId(request.getGroupId())) == null) {
            model = modelAttributesService.usersAttributes(model, myUser, true, commentsService.loadComments("User", myUser.getLogin()));
            return "user/userPage";
        }

        model = modelAttributesService.groupsAttributes(model, myUser, admins, request.getGroupId(), owner);
        model.addAttribute("users", list);
        return "group/groupsMembers";
    }

    @PostMapping("/addToAdmins")
    public String addToAdmins(@ModelAttribute("request") GroupUserRequest request, Model model) throws IOException {
        User myUser = userService.findUserByLoginAndPassword(request.getLogin(), request.getPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        }

        groupService.addToAdmins(new ObjectId(request.getGroupId()), request.getUserId());

        List<UserSearchResult> list = groupService.findGroupsMembers(new ObjectId(request.getGroupId()));
        List<String> admins = groupService.findGroupsAdmins(new ObjectId(request.getGroupId()));
        String owner = groupService.findGroupOwner(new ObjectId(request.getGroupId()));

        if (groupService.findGroupById(new ObjectId(request.getGroupId())) == null) {
            model = modelAttributesService.usersAttributes(model, myUser, true, commentsService.loadComments("User", myUser.getLogin()));
            return "user/userPage";
        }

        model = modelAttributesService.groupsAttributes(model, myUser, admins, request.getGroupId(), owner);
        model.addAttribute("users", list);
        return "group/groupsMembers";
    }

    @PostMapping("/removeFromAdmins")
    public String removeFromAdmins(@ModelAttribute("request") GroupUserRequest request, Model model) throws IOException {
        User myUser = userService.findUserByLoginAndPassword(request.getLogin(), request.getPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        }

        groupService.removeFromAdmins(new ObjectId(request.getGroupId()), request.getUserId());

        List<UserSearchResult> list = groupService.findGroupsMembers(new ObjectId(request.getGroupId()));
        List<String> admins = groupService.findGroupsAdmins(new ObjectId(request.getGroupId()));
        String owner = groupService.findGroupOwner(new ObjectId(request.getGroupId()));

        if (groupService.findGroupById(new ObjectId(request.getGroupId())) == null) {
            model = modelAttributesService.usersAttributes(model, myUser, true, commentsService.loadComments("User", myUser.getLogin()));
            return "user/userPage";
        }

        model = modelAttributesService.groupsAttributes(model, myUser, admins, request.getGroupId(), owner);
        model.addAttribute("users", list);
        return "group/groupsMembers";
    }

    @PostMapping("/removeFromAdminsAdminsList")
    public String removeFromAdminsAdminsList(@ModelAttribute("request") GroupUserRequest request, Model model) throws IOException {
        User myUser = userService.findUserByLoginAndPassword(request.getLogin(), request.getPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        }

        groupService.removeFromAdmins(new ObjectId(request.getGroupId()), request.getUserId());

        List<UserSearchResult> list = groupService.findGroupsAdminsPresentable(new ObjectId(request.getGroupId()));
        String owner = groupService.findGroupOwner(new ObjectId(request.getGroupId()));

        if (groupService.findGroupById(new ObjectId(request.getGroupId())) == null) {
            model = modelAttributesService.usersAttributes(model, myUser, true, commentsService.loadComments("User", myUser.getLogin()));
            return "user/userPage";
        }

        model = modelAttributesService.groupsAttributesFullList(model, myUser, list, request.getGroupId(), owner);
        return "group/groupsAdmins";
    }

    @GetMapping("/getGroupUpdatingForm")
    public String getGroupUpdatingForm(LeftFrameRequest request, Model model) throws IOException {
        User user = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        Group groupFounded = groupService.findGroupById(new ObjectId(request.getId()));
        GroupSearchResult group = modelMapper.map(groupFounded, GroupSearchResult.class);
        group.setId(groupFounded.getId().toString());
        String file;
        if (groupFounded.getImageId() == null || groupFounded.getImageId().isEmpty()) {
            file = null;
        } else {
            GridFsResource resource = fileStorageService.getFileById(groupFounded.getImageId());
            file = fileStorageService.convertGridFsFileToBase64(resource);
        }
        group.setFile(file);

        model.addAttribute("group", group);
        model.addAttribute("author", user);
        if (user.getImageId() != null && !user.getImageId().isEmpty()) {
            GridFsResource userAvatar = fileStorageService.getFileById(user.getImageId());
            model.addAttribute("avatar", fileStorageService.convertGridFsFileToBase64(userAvatar));
        } else {
            model.addAttribute("avatar", null);
        }

        return "group/groupUpdatingForm";
    }

    @PostMapping("/updateGroup")
    public String updateGroup(User user, Group group, String groupId, MultipartFile file, Model model) throws IOException {
        user = userService.findUserByLoginAndPassword(user.getLogin(), user.getPassword());

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        group.setId(new ObjectId(groupId));
        groupService.updateGroup(group, file);

        return "redirect:/searchResult/getGroupPage?login=" + user.getLogin() +
                "&password=" + user.getPassword() + "&groupId=" + group.getId();
    }

}
