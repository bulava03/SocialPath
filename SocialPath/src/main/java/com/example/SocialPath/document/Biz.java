package com.example.SocialPath.document;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

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

    private String concreteAddress;
    private double latitude;
    private double longitude;
    private boolean onlyOnline;
}
