package com.example.SocialPath.extraClasses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicationPresentable {
    private ObjectId id;
    private String authorLogin;
    private String authorName;
    private String authorAvatar;
    private String text;
    private LocalDateTime createdAt;
    private List<String> media;
    private boolean isVideo;
    private List<PublicationPresentable> publications;
}
