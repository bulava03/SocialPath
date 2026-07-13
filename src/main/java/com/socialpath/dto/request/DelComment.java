package com.socialpath.dto.request;

import lombok.Data;
import org.bson.types.ObjectId;

@Data
public class DelComment {
    private String login;
    private String groupId;
    private String authorLogin;
    private String idPublication;
    private ObjectId idComment;
}
