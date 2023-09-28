package com.example.SocialPath_Admin.document;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Document(collection = "report")
public class Report {
    @Id
    private ObjectId id;
    private String author;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime date;
    private String type;
    private String subject;
    private String idPublication;
    private ObjectId idComment;
    private String idUser;
    private ObjectId idGroup;
    private String status;
    private String result;
}
