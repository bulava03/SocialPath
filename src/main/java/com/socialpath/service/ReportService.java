package com.socialpath.service;

import com.socialpath.entity.Report;

/**
 * Stores moderation reports raised against comments or groups.
 */
public interface ReportService {

    /**
     * Persists a report.
     * @param report the report to store
     */
    void addReport(Report report);
}
