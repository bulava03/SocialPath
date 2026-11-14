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
    /** The video can be played. */
    public static final String VIDEO_READY = "READY";
    /** The video is waiting for or going through conversion. */
    public static final String VIDEO_PROCESSING = "PROCESSING";
    /** The video could not be converted. */
    public static final String VIDEO_FAILED = "FAILED";

    private Long id;
    private String authorLogin;
    private String authorName;
    private String authorImageId;
    private String text;
    private LocalDateTime createdAt;
    private List<String> media;
    private boolean isVideo;
    /** One of the VIDEO_* values for a video, null otherwise. */
    private String videoStatus;
    /** The loaded portion of direct replies, newest first; possibly not all of them. */
    private List<PublicationPresentable> publications;
    /** Direct replies not loaded yet, shown behind a button. */
    private long hiddenReplies;
    /** How many of the hidden replies the next portion brings. */
    private long nextReplies;
    /** Where the hidden replies continue (below this id); null when none are loaded yet. */
    private Long repliesBefore;
}
