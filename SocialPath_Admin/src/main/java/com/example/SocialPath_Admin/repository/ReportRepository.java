package com.example.SocialPath_Admin.repository;

import com.example.SocialPath_Admin.document.Report;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends MongoRepository<Report, ObjectId> {

    List<Report> findByStatus(String status);

    List<Report> findByIdComment(ObjectId idComment);

    default void changeStatusToInReview(ObjectId reportId) {
        Report report = findById(reportId).orElse(null);

        if (report != null) {
            report.setStatus("In Review");
            save(report);
        }
    }

}
