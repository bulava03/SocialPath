package com.example.SocialPath_Admin.document;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "publication")
@NoArgsConstructor
public class Publication {
    @Id
    private ObjectId id;
    private String text;
    private String authorId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdAt;
    private List<ObjectId> comments;
}
