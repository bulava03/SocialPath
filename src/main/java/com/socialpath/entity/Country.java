package com.socialpath.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * A country from the read-only reference table seeded by the V4 migration.
 * The code is the ISO 3166-1 alpha-2 code (XK for Kosovo).
 */
@Data
@Entity
@Table(name = "countries")
public class Country {
    @Id
    @Column(name = "code", length = 2, nullable = false)
    private String code;

    @Column(name = "name", length = 64, nullable = false, unique = true)
    private String name;
}
