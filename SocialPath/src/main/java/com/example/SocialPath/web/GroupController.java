package com.example.SocialPath.web;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.*;
import com.example.SocialPath.helper.CheckHelper;
import com.example.SocialPath.security.JwtTokenProvider;
import com.example.SocialPath.service.HandleAvatarService;
import com.example.SocialPath.service.*;
import jakarta.servlet.http.HttpServletRequest;
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
    private HandleAvatarService handleAvatarService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @GetMapping("/getGroupCreationForm")
    public String getGroupCreationForm(HttpServletRequest req, Model model) {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(login);

        model.addAttribute("author", new UserLogin(myUser.getLogin(), myUser.getPassword()));
        model.addAttribute("group", new GroupCreationForm(myUser.getLogin(), myUser.getPassword()));
        return "group/groupCreationForm";
    }

    @PostMapping("/createGroup")
    public String createGroup(HttpServletRequest req, GroupCreationForm groupCreationForm, Model model) {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(login);

        Object[] validation = groupService.validateGroup(groupCreationForm);
        if (!(boolean) validation[0]) {
            model.addAttribute("user", new UserLogin(myUser.getLogin(), myUser.getPassword()));
            model.addAttribute("author", new UserLogin(myUser.getLogin(), myUser.getPassword()));
            model.addAttribute("group", groupCreationForm);
            model.addAttribute("errorText", validation[1].toString().replaceAll("Optional\\[|\\]", ""));
            return "group/groupCreationForm";
        }
        if (userService.findUserByLoginAndPassword(groupCreationForm.getOwner(), groupCreationForm.getOwnerPassword()) != null) {
            Group group = modelMapper.map(groupCreationForm, Group.class);
            group = groupService.addGroup(group);
            return "redirect:/searchResult/getGroupPage?groupId=" + group.getId();
        }
        return "redirect:/user/authorisation";
    }

    @GetMapping("/getGroupsMembers")
    public String getGroupsMembers(HttpServletRequest req, LeftFrameRequest request, Model model) throws IOException {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User user = userService.findByLogin(login);

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        List<UserSearchResult> list = groupService.findGroupsMembers(new ObjectId(request.getId()));
        List<String> admins = groupService.findGroupsAdmins(new ObjectId(request.getId()));
        String owner = groupService.findGroupOwner(new ObjectId(request.getId()));

        if (groupService.findGroupById(new ObjectId(request.getId())) == null) {
            return "redirect:/user/authorisation";
        }

        model = modelAttributesService.groupsAttributes(model, user, admins, request.getId(), owner);
        model.addAttribute("users", list);
        return "group/groupsMembers";
    }

    @GetMapping("/getGroupsAdmins")
    public String getGroupsAdmins(HttpServletRequest req, LeftFrameRequest request, Model model) throws IOException {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User user = userService.findByLogin(login);

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        List<UserSearchResult> list = groupService.findGroupsAdminsPresentable(new ObjectId(request.getId()));
        String owner = groupService.findGroupOwner(new ObjectId(request.getId()));

        if (groupService.findGroupById(new ObjectId(request.getId())) == null) {
            return "redirect:/user/authorisation";
        }

        model = modelAttributesService.groupsAttributesFullList(model, user, list, request.getId(), owner);
        return "group/groupsAdmins";
    }

    @PostMapping("/removeUserFromGroup")
    public String removeUserFromGroup(HttpServletRequest req, GroupUserRequest request, Model model) throws IOException {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(login);

        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        }

        groupService.removeUserFromGroup(new ObjectId(request.getGroupId()), request.getUserId());

        if (groupService.findGroupById(new ObjectId(request.getGroupId())) == null) {
            return "redirect:/user/authorisation";
        }

        return "redirect:/group/getGroupsMembers?id=" + request.getGroupId();
    }

    @PostMapping("/addToAdmins")
    public String addToAdmins(HttpServletRequest req, GroupUserRequest request, Model model) throws IOException {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(login);

        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        }

        groupService.addToAdmins(new ObjectId(request.getGroupId()), request.getUserId());

        if (groupService.findGroupById(new ObjectId(request.getGroupId())) == null) {
            return "redirect:/user/authorisation";
        }

        return "redirect:/group/getGroupsMembers?id=" + request.getGroupId();
    }

    @PostMapping("/removeFromAdmins")
    public String removeFromAdmins(HttpServletRequest req, GroupUserRequest request, Model model) throws IOException {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(login);

        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        }

        groupService.removeFromAdmins(new ObjectId(request.getGroupId()), request.getUserId());

        if (groupService.findGroupById(new ObjectId(request.getGroupId())) == null) {
            return "redirect:/user/authorisation";
        }

        return "redirect:/group/getGroupsMembers?id=" + request.getGroupId();
    }

    @PostMapping("/removeFromAdminsAdminsList")
    public String removeFromAdminsAdminsList(HttpServletRequest req, GroupUserRequest request, Model model) throws IOException {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(login);

        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        }

        groupService.removeFromAdmins(new ObjectId(request.getGroupId()), request.getUserId());

        if (groupService.findGroupById(new ObjectId(request.getGroupId())) == null) {
            return "redirect:/user/authorisation";
        }

        return "redirect:/group/getGroupsAdmins?id=" + request.getGroupId();
    }

    @GetMapping("/getGroupUpdatingForm")
    public String getGroupUpdatingForm(HttpServletRequest req, LeftFrameRequest request, Model model) throws IOException {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User user = userService.findByLogin(login);

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        Group groupFounded = groupService.findGroupById(new ObjectId(request.getId()));
        GroupSearchResult group = modelMapper.map(groupFounded, GroupSearchResult.class);
        group.setId(groupFounded.getId().toString());
        group = handleAvatarService.updateAvatar(groupFounded, group);

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
    public String updateGroup(HttpServletRequest req, Group group, String groupId, MultipartFile file, Model model) throws IOException {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User user = userService.findByLogin(login);

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        Group groupFounded = groupService.findGroupById(new ObjectId(groupId));

        if (groupFounded.getOwner() != null && groupFounded.getOwner().equals(user.getLogin())) {
            group.setId(new ObjectId(groupId));
            groupService.updateGroup(group, file);
        }

        return "redirect:/searchResult/getGroupPage?groupId=" + group.getId();
    }

}
