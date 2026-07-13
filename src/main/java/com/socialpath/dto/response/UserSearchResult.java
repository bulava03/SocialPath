package com.socialpath.dto.response;

import lombok.Data;


@Data
public class UserSearchResult {
    private String login;
    private String anotherUserLogin;
    private String imageId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String country;
    private String region;
    private String city;
    private String education;
    private String workplace;
}
