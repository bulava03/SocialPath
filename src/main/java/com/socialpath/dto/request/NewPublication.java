package com.socialpath.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class NewPublication {
    private String groupId;
    private String authorId;
    private String text;
    private List<MultipartFile> media;
}
