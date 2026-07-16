package com.socialpath;

import com.socialpath.entity.Report;
import com.socialpath.dto.request.NewReport;
import com.socialpath.helper.ReportHelper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportHelperTest {

    @Test
    void convertNewReportToReport_WithCommentUserType_ReturnsCorrectReport() {
        // Arrange
        NewReport newReport = new NewReport();
        newReport.setAuthorLogin("testAuthor");
        newReport.setType("Comment User");
        newReport.setSubject("Test Subject");
        newReport.setIdPublication("publications");
        newReport.setIdComment("comment123");
        newReport.setIdPage("user123");

        // Act
        Report result = ReportHelper.ConvertNewReportToReport(newReport);

        // Assert
        assertEquals("testAuthor", result.getAuthor());
        assertEquals("Comment User", result.getType());
        assertEquals("Test Subject", result.getSubject());
        assertEquals("publications", result.getIdPublication());
        assertEquals("comment123", result.getIdComment());
        assertEquals("user123", result.getIdUser());
        assertEquals("Free", result.getStatus());
        assertNotNull(result.getDate());
        assertTrue(result.getDate().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(result.getDate().isAfter(LocalDateTime.now().minusMinutes(1)));
        assertNull(result.getIdGroup());
    }

    @Test
    void convertNewReportToReport_WithCommentUserTypeNotPublications_ReturnsCorrectReport() {
        // Arrange
        NewReport newReport = new NewReport();
        newReport.setAuthorLogin("testAuthor");
        newReport.setType("Comment User");
        newReport.setSubject("Test Subject");
        newReport.setIdPublication("publication123");
        newReport.setIdComment("comment123");
        newReport.setIdPage("user123");

        // Act
        Report result = ReportHelper.ConvertNewReportToReport(newReport);

        // Assert
        assertEquals("testAuthor", result.getAuthor());
        assertEquals("Comment User", result.getType());
        assertEquals("Test Subject", result.getSubject());
        assertEquals("publication123", result.getIdPublication());
        assertEquals("comment123", result.getIdComment());
        assertEquals("Free", result.getStatus());
        assertNull(result.getIdUser());
        assertNull(result.getIdGroup());
    }

    @Test
    void convertNewReportToReport_WithCommentGroupType_ReturnsCorrectReport() {
        // Arrange
        NewReport newReport = new NewReport();
        newReport.setAuthorLogin("testAuthor");
        newReport.setType("Comment Group");
        newReport.setSubject("Test Subject");
        newReport.setIdPublication("publications");
        newReport.setIdComment("comment123");
        newReport.setIdPage("group123");

        // Act
        Report result = ReportHelper.ConvertNewReportToReport(newReport);

        // Assert
        assertEquals("testAuthor", result.getAuthor());
        assertEquals("Comment Group", result.getType());
        assertEquals("Test Subject", result.getSubject());
        assertEquals("publications", result.getIdPublication());
        assertEquals("comment123", result.getIdComment());
        assertEquals("group123", result.getIdGroup());
        assertEquals("Free", result.getStatus());
        assertNull(result.getIdUser());
    }

    @Test
    void convertNewReportToReport_WithOtherType_ReturnsBasicReport() {
        // Arrange
        NewReport newReport = new NewReport();
        newReport.setAuthorLogin("testAuthor");
        newReport.setType("Other Type");
        newReport.setSubject("Test Subject");
        newReport.setIdPage("page123");

        // Act
        Report result = ReportHelper.ConvertNewReportToReport(newReport);

        // Assert
        assertEquals("testAuthor", result.getAuthor());
        assertEquals("Other Type", result.getType());
        assertEquals("Test Subject", result.getSubject());
        assertEquals("page123", result.getIdGroup());
        assertEquals("Free", result.getStatus());
        assertNotNull(result.getDate());
        assertNull(result.getIdComment());
        assertNull(result.getIdPublication());
        assertNull(result.getIdUser());
    }
}
