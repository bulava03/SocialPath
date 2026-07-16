package com.socialpath.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NewComment {
    private Long id;
    private String groupId;
    private String authorLogin;
    private String text;
    private Long idPublication;
    private LocalDateTime createdAt;
    private List<MultipartFile> media;
}
