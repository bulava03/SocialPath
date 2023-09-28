package com.example.SocialPath.document;

import lombok.AllArgsConstructor;
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
    private String idComment;
    private String idUser;
    private String idGroup;
    private String status;
    private String result;

    public Report(String author, String type, String subject, String idPublication, String idComment) {
        this.author = author;
        this.date = LocalDateTime.now();
        this.subject = subject;
        this.type = type;
        this.idPublication = idPublication;
        this.idComment = idComment;
        this. status = "Free";
    }

    public Report(String author, String type, String subject, String idGroup) {
        this.author = author;
        this.date = LocalDateTime.now();
        this.subject = subject;
        this.type = type;
        this.idGroup = idGroup;
        this. status = "Free";
    }
}
