package com.socialpath.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicationPresentable {
    private Long id;
    private String authorLogin;
    private String authorName;
    private String authorImageId;
    private String text;
    private LocalDateTime createdAt;
    private List<String> media;
    private boolean isVideo;
    private List<PublicationPresentable> publications;
}
