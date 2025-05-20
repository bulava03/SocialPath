package com.example.SocialPath.document;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "grades")
public class Grade {
    @Id
    private ObjectId id;
    private String userLogin;
    private String bizLogin;
    private int rating;
    private String comment;
}
