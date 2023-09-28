package com.example.SocialPath_SuperAdmin.document;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Document(collection = "report")
public class Report {
    @Id
    @Field("_id")
    private ObjectId id;

    @Field("ObjectIdInType")
    private String objectIdInType;

    @Field("Author")
    private String author;

    @Field("Date")
    private LocalDateTime date;

    @Field("Subject")
    private String subject;

    @Field("Type")
    private String type;

    @Field("IdPublication")
    private String idPublication;

    @Field("IdComment")
    private String idComment;

    @Field("Result")
    private String result;

    @Field("Status")
    private String status;
}
