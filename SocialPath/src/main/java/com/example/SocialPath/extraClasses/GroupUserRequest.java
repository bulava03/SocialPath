package com.example.SocialPath.extraClasses;

import lombok.Data;

@Data
public class GroupUserRequest {
    private String login;
    private String password;
    private String groupId;
    private String userId;
}
