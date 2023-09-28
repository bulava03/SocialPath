package com.example.SocialPath_Admin.service;

import com.example.SocialPath_Admin.document.Report;
import org.bson.types.ObjectId;

import java.util.List;

public interface ReportService {
    boolean checkIfExists(ObjectId id);
    boolean checkIfExistsAndInReview(ObjectId id);
    Report findById(ObjectId id);
    void changeStatusToInReview(Report report);
    List<Report> getReportsWithStatusFree();
    void removeReportsByIds(List<ObjectId> reportIds);
    void deleteReportsByIdComment(ObjectId idComment, ObjectId idNotToDelete);
}
