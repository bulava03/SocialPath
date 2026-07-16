package com.socialpath.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Data;

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
}
