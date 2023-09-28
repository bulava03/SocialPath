package com.example.SocialPath.extraClasses;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
public class FoundedGroup {
    private String login;
    private String password;
    private String groupId;
}