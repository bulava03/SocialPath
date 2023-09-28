package com.example.SocialPath.extraClasses;

import lombok.Data;

@Data
public class SearchRequest {
    private String login;
    private String password;
    private String searchText;
}
