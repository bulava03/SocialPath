package com.example.SocialPath.extraClasses;

import lombok.Data;
import org.bson.types.ObjectId;

import java.util.List;

@Data
public class UserFormUpdate {
    private String login;
    private String password;
    private String passwordOld;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private int day;
    private String month;
    private int year;
    private String country;
    private String region;
    private String city;
    private String education;
    private String workplace;
    private List<ObjectId> groups;
    private List<String> friends;
    private List<String> friendInvites;
    private List<ObjectId> publications;
}
