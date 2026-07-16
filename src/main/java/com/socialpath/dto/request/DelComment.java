package com.socialpath.dto.request;

import lombok.Data;

@Data
public class DelComment {
    private String login;
    private String groupId;
    private String authorLogin;
    private String idPublication;
    private Long idComment;
}
