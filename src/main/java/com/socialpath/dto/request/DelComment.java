package com.socialpath.dto.request;

import lombok.Data;

@Data
public class DelComment {
    private String groupId;
    private String authorLogin;
    private Long idComment;
}
