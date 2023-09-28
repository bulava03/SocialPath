package com.example.SocialPath.document;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "group")
public class Group {
    @Id
    private ObjectId id;
    private String name;
    private String owner;
    private List<String> members;
    private List<String> admins;
    private List<ObjectId> publications;
}
