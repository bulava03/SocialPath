package com.example.SocialPath.extraClasses;

import lombok.Data;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

@Data
public class NewComment {
    private  ObjectId id;
    private String login;
    private String groupId;
    private String authorLogin;
    private String authorPassword;
    private String text;
    private ObjectId idPublication;
    private LocalDateTime createdAt;
}
