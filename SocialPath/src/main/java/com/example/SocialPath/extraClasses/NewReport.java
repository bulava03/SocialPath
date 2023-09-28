package com.example.SocialPath.extraClasses;

import lombok.Data;

@Data
public class NewReport {
    private String authorLogin;
    private String authorPassword;
    private String subject;
    private String type;
    private String idPublication;
    private String idComment;
    private String idPage;
}
