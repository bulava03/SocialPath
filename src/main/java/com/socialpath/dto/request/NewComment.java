package com.socialpath.dto.request;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NewComment {
    private  ObjectId id;
    private String groupId;
    private String authorLogin;
    private String text;
    private ObjectId idPublication;
    private LocalDateTime createdAt;
    private List<MultipartFile> media;
}
