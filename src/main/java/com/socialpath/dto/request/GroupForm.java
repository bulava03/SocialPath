package com.socialpath.dto.request;

import com.socialpath.entity.Group;
import com.socialpath.entity.GroupCategory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * What the owner fills in when creating or editing a group. Also backs the
 * form when it is shown again after a validation error, so the values typed
 * are kept.
 */
@Data
public class GroupForm {

    @NotBlank(message = "{group.name.required}")
    @Size(max = 50, message = "{group.name.size}")
    private String name;

    @Size(max = 1000, message = "{group.description.size}")
    private String description;

    /** Null when no topic is chosen (the form's empty option). */
    private GroupCategory category;

    /** The owner's own topic; asked for, and kept, only with OTHER. */
    @Size(max = 30, message = "{group.customCategory.size}")
    private String customCategory;

    /**
     * Only http(s) links: anything else (javascript:, data:) could run on a
     * click once it is rendered as a link.
     */
    @Size(max = 255, message = "{group.website.size}")
    @Pattern(regexp = "^$|^https?://[^\\s/?#]+\\.[^\\s]+$", flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "{group.website.invalid}")
    private String website;

    /**
     * "Other" alone tells nothing about a group, so it needs its own name.
     * @return false when OTHER is chosen without one
     */
    @AssertTrue(message = "{group.customCategory.required}")
    public boolean isCustomCategoryGivenForOther() {
        return category != GroupCategory.OTHER || (customCategory != null && !customCategory.isBlank());
    }

    /**
     * The form filled with a group's current details, for the edit page.
     * @param group the stored group
     * @return the filled form
     */
    public static GroupForm from(Group group) {
        GroupForm form = new GroupForm();
        form.setName(group.getName());
        form.setDescription(group.getDescription());
        form.setCategory(group.getCategory());
        form.setCustomCategory(group.getCustomCategory());
        form.setWebsite(group.getWebsite());
        return form;
    }

    /**
     * Copies the details into a group. Surrounding blanks are dropped, an
     * empty description or link is stored as none, and the own topic only
     * stays with OTHER (it may have been typed before another topic was chosen).
     * @param group the group to change
     */
    public void applyTo(Group group) {
        group.setName(name.strip());
        group.setDescription(blankToNull(description));
        group.setCategory(category);
        group.setCustomCategory(category == GroupCategory.OTHER ? blankToNull(customCategory) : null);
        group.setWebsite(blankToNull(website));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
