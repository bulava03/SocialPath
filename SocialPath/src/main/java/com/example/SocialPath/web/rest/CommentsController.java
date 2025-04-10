package com.example.SocialPath.web.rest;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.DelComment;
import com.example.SocialPath.extraClasses.NewComment;
import com.example.SocialPath.extraClasses.NewPublication;
import com.example.SocialPath.security.JwtTokenProvider;
import com.example.SocialPath.service.CommentsService;
import com.example.SocialPath.service.GroupService;
import com.example.SocialPath.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/comments")
public class CommentsController {

    @Autowired
    private UserService userService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private CommentsService commentsService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/addUserPublication")
    public int addUserPublication(HttpServletRequest request, NewPublication publication) {
        String token = userService.resolveToken(request);
        if (token == null) {
            return -1;
        }
        String authorLogin = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(authorLogin);
        if (myUser == null) {
            return -1;
        }

        if (myUser.getBan() != null && !myUser.getBan().isAfter(LocalDateTime.now())) {
            return -1;
        }

        if (publication.getText() != null && !publication.getText().trim().isEmpty()) {
            publication.setAuthorId(myUser.getLogin());
            commentsService.addNewUserPublication(publication);
            return 0;
        }

        return -1;
    }

    @PostMapping("/addCommentUser")
    public int addCommentUser(HttpServletRequest request, NewComment newComment) {
        String token = userService.resolveToken(request);
        if (token == null) {
            return -1;
        }
        String authorLogin = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(authorLogin);
        if (myUser == null) {
            return -1;
        }

        if (myUser.getBan() != null && !myUser.getBan().isAfter(LocalDateTime.now())) {
            return -1;
        }

        if (newComment.getText() != null && !newComment.getText().trim().isEmpty() && commentsService.findById(newComment.getIdPublication()) != null) {
            newComment.setAuthorLogin(myUser.getLogin());
            commentsService.addNewComment(newComment);
            return 0;
        }

        return -1;
    }

    @PostMapping("/removeCommentUser")
    public int removeCommentUser(HttpServletRequest request, DelComment delComment) {
        String token = userService.resolveToken(request);
        if (token == null) {
            return -1;
        }
        String authorLogin = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(authorLogin);
        if (myUser == null) {
            return -1;
        }

        if (myUser.getBan() != null && !myUser.getBan().isAfter(LocalDateTime.now())) {
            return -1;
        }

        if (commentsService.findById(delComment.getIdComment()) != null) {
            if (delComment.getIdPublication().equals("publications")) {
                delComment.setLogin(myUser.getLogin());
                commentsService.removePublicationUser(delComment);
                return 0;
            } else if (commentsService.findById(new ObjectId(delComment.getIdPublication())) != null) {
                commentsService.removeComment(delComment);
                return 0;
            }
        }

        return -1;
    }

    @PostMapping("/addGroupPublication")
    public int addGroupPublication(HttpServletRequest request, NewPublication publication) {
        String token = userService.resolveToken(request);
        if (token == null) {
            return -1;
        }
        String authorLogin = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(authorLogin);
        if (myUser == null) {
            return -1;
        }

        Group myGroup = groupService.findGroupById(new ObjectId(publication.getGroupId()));
        if (myGroup == null) {
            return -1;
        }

        if (myUser.getBan() != null && !myUser.getBan().isAfter(LocalDateTime.now())) {
            return -1;
        }

        if (publication.getText() != null && !publication.getText().trim().isEmpty()) {
            publication.setAuthorId(myUser.getLogin());
            commentsService.addNewGroupPublication(publication);
            return 0;
        }

        return -1;
    }

    @PostMapping("/addCommentGroup")
    public int addCommentGroup(HttpServletRequest request, NewComment newComment) {
        String token = userService.resolveToken(request);
        if (token == null) {
            return -1;
        }
        String authorLogin = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(authorLogin);
        if (myUser == null) {
            return -1;
        }

        Group myGroup = groupService.findGroupById(new ObjectId(newComment.getGroupId()));
        if (myGroup == null) {
            return -1;
        }

        if (myUser.getBan() != null && !myUser.getBan().isAfter(LocalDateTime.now())) {
            return -1;
        }

        if (newComment.getText() != null && !newComment.getText().trim().isEmpty() && commentsService.findById(newComment.getIdPublication()) != null) {
            newComment.setAuthorLogin(myUser.getLogin());
            commentsService.addNewComment(newComment);
            return 0;
        }

        return -1;
    }

    @PostMapping("/removeCommentGroup")
    public int removeCommentGroup(HttpServletRequest request, DelComment delComment) {
        String token = userService.resolveToken(request);
        if (token == null) {
            return -1;
        }
        String authorLogin = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(authorLogin);
        if (myUser == null) {
            return -1;
        }

        Group myGroup = groupService.findGroupById(new ObjectId(delComment.getGroupId()));
        if (myGroup == null) {
            return -1;
        }

        if (myUser.getBan() != null && !myUser.getBan().isAfter(LocalDateTime.now())) {
            return -1;
        }

        if (commentsService.findById(delComment.getIdComment()) != null) {
            if (delComment.getIdPublication().equals("publications")) {
                delComment.setLogin(myUser.getLogin());
                commentsService.removePublicationGroup(delComment);
                return 0;
            } else if (commentsService.findById(new ObjectId(delComment.getIdPublication())) != null) {
                commentsService.removeComment(delComment);
                return 0;
            }
        }

        return -1;
    }

}
