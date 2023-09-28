package com.example.SocialPath.extraClasses;

import lombok.Data;
import org.bson.types.ObjectId;

@Data
public class DelComment {
    private String login;
    private String authorLogin;
    private String authorPassword;
    private String idPublication;
    private ObjectId idComment;
}
