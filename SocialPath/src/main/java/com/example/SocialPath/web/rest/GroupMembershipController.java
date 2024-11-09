package com.example.SocialPath.web.rest;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.document.User;
import com.example.SocialPath.security.JwtTokenProvider;
import com.example.SocialPath.service.GroupService;
import com.example.SocialPath.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groupMembership")
public class GroupMembershipController {

    @Autowired
    private UserService userService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/joinGroup")
    public int joinGroup(HttpServletRequest request, String groupId) {
        String token = userService.resolveToken(request);
        if (token == null) {
            return -1;
        }
        String authorLogin = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(authorLogin);

        if (myUser == null) {
            return -1;
        }

        Group group = groupService.findGroupById(new ObjectId(groupId));

        if (group == null) {
            return -1;
        }

        if (group.getMembers() != null) {
            if (group.getMembers().contains(authorLogin)) {
                return 0;
            }
        }

        groupService.addMember(groupId, authorLogin);

        if (myUser.getGroups() == null || !myUser.getGroups().contains(new ObjectId(groupId))) {
            userService.addGroup(authorLogin, groupId);
        }

        return 1;
    }

    @PostMapping("/leaveGroup")
    public int leaveGroup(HttpServletRequest request, String groupId) {
        String token = userService.resolveToken(request);
        if (token == null) {
            return -1;
        }
        String authorLogin = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(authorLogin);

        if (myUser == null) {
            return -1;
        }

        Group group = groupService.findGroupById(new ObjectId(groupId));

        if (group == null) {
            return -1;
        }

        if (group.getMembers() != null) {
            if (!group.getMembers().contains(authorLogin)) {
                return 0;
            }
        }

        groupService.removeMember(groupId, authorLogin);

        if (myUser.getGroups() != null && myUser.getGroups().contains(new ObjectId(groupId))) {
            userService.removeGroup(authorLogin, groupId);
        }

        return 1;
    }

}
