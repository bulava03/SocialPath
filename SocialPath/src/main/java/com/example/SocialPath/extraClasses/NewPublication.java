package com.example.SocialPath.extraClasses;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class NewPublication {
    private String login;
    private String groupId;
    private String password;
    private String authorId;
    private String authorPassword;
    private String type;
    private String idInType;
    private String text;
    private List<MultipartFile> media;
}
