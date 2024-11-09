package com.example.SocialPath.web;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.DelComment;
import com.example.SocialPath.extraClasses.NewComment;
import com.example.SocialPath.extraClasses.NewPublication;
import com.example.SocialPath.extraClasses.UserLogin;
import com.example.SocialPath.helper.CheckHelper;
import com.example.SocialPath.service.CommentsService;
import com.example.SocialPath.service.GroupService;
import com.example.SocialPath.service.ModelAttributesService;
import com.example.SocialPath.service.UserService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/comments")
public class CommentsController {
    @Autowired
    private UserService userService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private CommentsService commentsService;
    @Autowired
    private ModelAttributesService modelAttributesService;

    @PostMapping("/addUserPublication")
    public String addUserPublication(@ModelAttribute("publication") NewPublication publication, Model model) {
        User myUser = userService.findUserByLoginAndPassword(publication.getLogin(), publication.getPassword());
        if (!publication.getLogin().equals(publication.getAuthorId())
                || !publication.getPassword().equals(publication.getAuthorPassword()) || myUser == null) {
            model.addAttribute("errorText", "Виникла помилка");
            return "home/index";
        } else if (myUser.getBan() == null || myUser.getBan().isAfter(LocalDateTime.now())) {
            model.addAttribute("errorText", "Цей акаунт заблоковано");
            return "home/index";
        } else if (publication.getText() != null && !publication.getText().trim().isEmpty()) {
            commentsService.addNewUserPublication(publication);
        }
        model = modelAttributesService.usersAttributes(model, userService.findUserById(publication.getLogin()), true, commentsService.loadComments("User", publication.getLogin()));
        return "user/userPage";
    }

    @PostMapping("/addGroupPublication")
    public String addGroupPublication(@ModelAttribute("publication") NewPublication publication, Model model) {
        User myUser = userService.findUserByLoginAndPassword(publication.getAuthorId(), publication.getAuthorPassword());
        Group myGroup = groupService.findGroupById(new ObjectId(publication.getLogin()));
        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        } else if (myGroup == null) {
            model = modelAttributesService.usersAttributes(model, myUser, true, commentsService.loadComments("User", publication.getAuthorId()));
            return "user/userPage";
        } else if (publication.getText() != null && !publication.getText().trim().isEmpty()) {
            commentsService.addNewGroupPublication(publication);
        }
        model.addAttribute("group", groupService.findGroupById(new ObjectId(publication.getLogin())));
        model.addAttribute("author", new UserLogin(publication.getAuthorId(), publication.getAuthorPassword()));
        model.addAttribute("publications", commentsService.loadComments("Group", publication.getLogin()));
        return "group/groupPage";
    }

    @PostMapping("/addCommentUser")
    public String addCommentUser(@ModelAttribute("newComment") NewComment newComment, Model model) {
        User myUser = userService.findUserByLoginAndPassword(newComment.getAuthorLogin(), newComment.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        } else if (newComment.getText() != null && !newComment.getText().trim().isEmpty() && commentsService.findById(newComment.getIdPublication()) != null) {
            commentsService.addNewComment(newComment);
        }

        model.addAttribute("user", userService.findUserById(newComment.getLogin()));
        model.addAttribute("author", userService.findUserByLoginAndPassword(newComment.getAuthorLogin(), newComment.getAuthorPassword()));
        model.addAttribute("isAuthor", newComment.getLogin().equals(newComment.getAuthorLogin()));
        if (!newComment.getLogin().equals(newComment.getAuthorLogin())) {
            User user = userService.findUserById(newComment.getLogin());
            user.setPassword("");

            myUser = userService.findUserById(newComment.getAuthorLogin());

            model.addAttribute("InRequests", CheckHelper.inRequestsCheck(user, myUser, newComment.getAuthorLogin(), newComment.getLogin()));
        }
        model.addAttribute("publications", commentsService.loadComments("User", newComment.getLogin()));

        return "user/userPage";
    }

    @PostMapping("/addCommentGroup")
    public String addCommentGroup(@ModelAttribute("newComment") NewComment newComment, Model model) {
        User myUser = userService.findUserByLoginAndPassword(newComment.getAuthorLogin(), newComment.getAuthorPassword());
        Group myGroup = groupService.findGroupById(new ObjectId(newComment.getLogin()));
        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        } else if (myGroup == null) {
            model = modelAttributesService.usersAttributes(model, myUser, true, commentsService.loadComments("User", newComment.getAuthorLogin()));
            return "user/userPage";
        } else if (newComment.getText() != null && !newComment.getText().trim().isEmpty() && commentsService.findById(newComment.getIdPublication()) != null) {
            commentsService.addNewComment(newComment);
        }
        model.addAttribute("group", groupService.findGroupById(new ObjectId(newComment.getLogin())));
        model.addAttribute("author", userService.findUserByLoginAndPassword(newComment.getAuthorLogin(), newComment.getAuthorPassword()));
        model.addAttribute("publications", commentsService.loadComments("Group", newComment.getLogin()));
        return "group/groupPage";
    }

    @PostMapping("/removeCommentUser")
    public String removeCommentUser(@ModelAttribute("delComment") DelComment delComment, Model model) {
        User myUser = userService.findUserByLoginAndPassword(delComment.getAuthorLogin(), delComment.getAuthorPassword());
        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        } else if (commentsService.findById(delComment.getIdComment()) != null) {
            if (delComment.getIdPublication().equals("publications")) {
                commentsService.removePublicationUser(delComment);
            } else if (commentsService.findById(new ObjectId(delComment.getIdPublication())) != null) {
                commentsService.removeComment(delComment);
            }
        }
        model.addAttribute("user", userService.findUserById(delComment.getLogin()));
        model.addAttribute("author", userService.findUserByLoginAndPassword(delComment.getAuthorLogin(), delComment.getAuthorPassword()));
        model.addAttribute("isAuthor", delComment.getLogin().equals(delComment.getAuthorLogin()));
        if (!delComment.getLogin().equals(delComment.getAuthorLogin())) {
            User user = userService.findUserById(delComment.getLogin());
            user.setPassword("");

            myUser = userService.findUserById(delComment.getAuthorLogin());

            model.addAttribute("InRequests", CheckHelper.inRequestsCheck(user, myUser, delComment.getAuthorLogin(), delComment.getLogin()));
        }
        model.addAttribute("publications", commentsService.loadComments("User", delComment.getLogin()));
        return "user/userPage";
    }

    @PostMapping("/removeCommentGroup")
    public String removeCommentGroup(@ModelAttribute("delComment") DelComment delComment, Model model) {
        User myUser = userService.findUserByLoginAndPassword(delComment.getAuthorLogin(), delComment.getAuthorPassword());
        Group myGroup = groupService.findGroupById(new ObjectId(delComment.getLogin()));
        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        } else if (myGroup == null) {
            model = modelAttributesService.usersAttributes(model, myUser, true, commentsService.loadComments("User", delComment.getAuthorLogin()));
            return "user/userPage";
        } else if (commentsService.findById(delComment.getIdComment()) != null) {
            if (delComment.getIdPublication().equals("publications")) {
                commentsService.removePublicationGroup(delComment);
            } else if (commentsService.findById(new ObjectId(delComment.getIdPublication())) != null) {
                commentsService.removeComment(delComment);
            }
        }
        model.addAttribute("group", groupService.findGroupById(new ObjectId(delComment.getLogin())));
        model.addAttribute("author", userService.findUserByLoginAndPassword(delComment.getAuthorLogin(), delComment.getAuthorPassword()));
        model.addAttribute("publications", commentsService.loadComments("Group", delComment.getLogin()));
        return "group/groupPage";
    }

}
