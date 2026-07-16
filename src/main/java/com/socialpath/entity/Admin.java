package com.socialpath.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "admins")
public class Admin {
    @Id
    @Column(name = "login", length = 20, nullable = false)
    private String login;

    @Column(name = "password", length = 72)
    private String password;

    @Column(name = "report_id")
    private Long report;
}
