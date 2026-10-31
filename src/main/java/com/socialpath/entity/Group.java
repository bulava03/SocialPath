package com.socialpath.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A group. Named social_groups in the schema because GROUPS is a reserved
 * word in MySQL 8. Membership and adminship are join tables keyed by login;
 * the owner is always present in both. Publications are not listed here —
 * they carry a group_id foreign key instead.
 */
@Data
@Entity
@Table(name = "social_groups")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_id", length = 64)
    private String imageId;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "owner_login", length = 20, nullable = false)
    private String owner;

    /** What the group is about, in the owner's words; null when not given. */
    @Column(name = "description", length = 1000)
    private String description;

    /** The topic from the fixed list; null when not chosen. */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    private GroupCategory category;

    /** The topic in the owner's words; only with {@link GroupCategory#OTHER}. */
    @Column(name = "custom_category", length = 30)
    private String customCategory;

    /** An http(s) link to the group's site or channel; null when not given. */
    @Column(name = "website", length = 255)
    private String website;

    /**
     * When the group was created. For groups older than this column it was
     * estimated from their first posts (see V8__group_details.sql).
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Logins of members, the owner included. EAGER: read in templates and auth checks. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "group_members", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "member_login", length = 20, nullable = false)
    private List<String> members;

    /** Logins of admins, the owner included. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "group_admins", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "admin_login", length = 20, nullable = false)
    private List<String> admins;

    /**
     * The topic as people see it: the owner's own words for OTHER, the
     * list's name otherwise.
     * @return e.g. "Games" or "Urban gardening", or null without a topic
     */
    public String getTopic() {
        if (category == GroupCategory.OTHER && customCategory != null) {
            return customCategory;
        }
        return category == null ? null : category.getLabel();
    }

    public boolean isOwner(String login) {
        return owner != null && owner.equals(login);
    }

    public boolean isAdmin(String login) {
        return admins != null && admins.contains(login);
    }

    public boolean isMember(String login) {
        return members != null && members.contains(login);
    }

    /**
     * Owner or admin: may moderate the group's publications.
     * @param login the user's login
     * @return true for the owner and for admins
     */
    public boolean isModerator(String login) {
        return isOwner(login) || isAdmin(login);
    }
}
