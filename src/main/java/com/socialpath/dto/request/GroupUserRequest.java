package com.socialpath.dto.request;

import lombok.Data;

@Data
public class GroupUserRequest {
    private String groupId;
    private String userId;
}
