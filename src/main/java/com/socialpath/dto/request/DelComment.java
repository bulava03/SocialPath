package com.socialpath.dto.request;

import lombok.Data;

/** A request to delete a post or comment, by the signed-in user. */
@Data
public class DelComment {
    /** The group named in the request, for a deletion in a group. */
    private String groupId;
    private Long idComment;
}
