package com.example.SocialPath.document;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.util.List;

@Data
public class Biz {
    @Id
    private ObjectId id;
    private String login;
    private String password;
    private String imageId;
    private String name;
    private String slogan;
    private String email;
    private String phoneNumber;
}
