package com.socialpath.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * A moderation report. The id* columns stay free-form strings on purpose:
 * the front-end sends the sentinel value "publications" in idPublication for
 * top-level publications, so these are report payload, not foreign keys.
 */
@Data
@Entity
@Table(name = "reports")
@NoArgsConstructor
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author", length = 20)
    private String author;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(name = "date")
    private LocalDateTime date;

    @Column(name = "type", length = 30)
    private String type;

    @Column(name = "subject", columnDefinition = "TEXT")
    private String subject;

    @Column(name = "id_publication", length = 64)
    private String idPublication;

    @Column(name = "id_comment", length = 64)
    private String idComment;

    @Column(name = "id_user", length = 20)
    private String idUser;

    @Column(name = "id_group", length = 64)
    private String idGroup;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    public Report(String author, String type, String subject, String idPublication, String idComment) {
        this.author = author;
        this.date = LocalDateTime.now();
        this.subject = subject;
        this.type = type;
        this.idPublication = idPublication;
        this.idComment = idComment;
        this.status = "Free";
    }

    public Report(String author, String type, String subject, String idGroup) {
        this.author = author;
        this.date = LocalDateTime.now();
        this.subject = subject;
        this.type = type;
        this.idGroup = idGroup;
        this.status = "Free";
    }
}
