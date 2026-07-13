package com.socialpath.dto.request;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
public class FoundedGroup {
    private String groupId;
}