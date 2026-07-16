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
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A publication or a comment — comments are publications with a parent
 * (adjacency list on parent_id), exactly mirroring the document model where
 * comments were publications referenced from a comments array.
 *
 * Placement:
 * - user-page publication: parentId == null, groupId == null, authorId is the page owner
 * - group publication:     parentId == null, groupId set
 * - comment (any depth):   parentId set
 *
 * Relations are plain foreign-key columns rather than object references; the
 * schema (Flyway V1) enforces them with ON DELETE CASCADE, and the service
 * layer walks the tree explicitly where it needs to clean up media files.
 */
@Data
@Entity
@Table(name = "publications")
@NoArgsConstructor
public class Publication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    /** Login of the author; kept as authorId to preserve the existing service/controller code. */
    @Column(name = "author_login", length = 20, nullable = false)
    private String authorId;

    /** Owning group for group publications, null for user-page publications and comments. */
    @Column(name = "group_id")
    private Long groupId;

    /** Parent publication for comments, null for top-level publications. */
    @Column(name = "parent_id")
    private Long parentId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Stored media file ids, in upload order. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "publication_media", joinColumns = @JoinColumn(name = "publication_id"))
    @OrderColumn(name = "media_order")
    @Column(name = "file_id", length = 64, nullable = false)
    private List<String> media;
}
