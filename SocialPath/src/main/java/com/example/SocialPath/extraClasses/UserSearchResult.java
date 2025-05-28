package com.example.SocialPath.extraClasses;

import lombok.Data;

@Data
public class UserSearchResult {
    private String login;
    private String anotherUserLogin;
    private String file;
    private String firstName;
    private String lastName;
    private String name;
    private String slogan;
    private String email;
    private String phoneNumber;
    private String country;
    private String region;
    private String city;
    private String education;
    private String workplace;
    private String concreteAddress;
    private double latitude;
    private double longitude;
    private boolean onlyOnline;
    private double score;
    private int matches;
}
