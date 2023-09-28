package com.example.SocialPath_Admin.extraClasses;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class ReportQuery {
    private ObjectId id;
    private String login;
    private String password;
    private String author;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime date;
    private String type;
    private String subject;
    private String idPublication;
    private String idComment;
    private String idUser;
    private String idGroup;
    private String result;
    private String status;
}
