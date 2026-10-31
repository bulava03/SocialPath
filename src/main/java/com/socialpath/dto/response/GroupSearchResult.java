package com.socialpath.dto.response;

import com.socialpath.entity.Group;
import lombok.Data;

/** A group as shown in lists and on the group edit form. */
@Data
public class GroupSearchResult {
    private String id;
    private String imageId;
    private String name;
    /** The topic as shown (see Group#getTopic), or null when none is chosen. */
    private String category;
    private int memberCount;

    public static GroupSearchResult from(Group group) {
        GroupSearchResult result = new GroupSearchResult();
        result.setId(group.getId().toString());
        result.setImageId(group.getImageId());
        result.setName(group.getName());
        result.setCategory(group.getTopic());
        result.setMemberCount(group.getMembers() == null ? 0 : group.getMembers().size());
        return result;
    }
}
