package com.socialpath.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** A new post as the form sends it; the author is always the signed-in user. */
@Data
public class NewPublication {
    /** The group to post in; unused for a post on the author's own page. */
    private String groupId;
    private String text;
    private List<MultipartFile> media;
}
