package com.example.SocialPath.extraClasses;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NewPublication {
    private String login;
    private String password;
    private String authorId;
    private String authorPassword;
    private String type;
    private String idInType;
    private String text;
}
