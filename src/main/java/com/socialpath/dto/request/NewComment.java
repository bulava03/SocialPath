package com.socialpath.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** A new comment as the form sends it; the author is always the signed-in user. */
@Data
public class NewComment {
    /** The group named in the request, for a comment in a group. */
    private String groupId;
    /** The post or comment replied to. */
    private Long idPublication;
    private String text;
    private List<MultipartFile> media;
}
