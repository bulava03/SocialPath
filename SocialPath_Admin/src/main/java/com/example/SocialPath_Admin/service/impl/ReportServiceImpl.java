package com.example.SocialPath_Admin.service.impl;

import com.example.SocialPath_Admin.document.Report;
import com.example.SocialPath_Admin.repository.AdminRepository;
import com.example.SocialPath_Admin.repository.ReportRepository;
import com.example.SocialPath_Admin.service.ReportService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private AdminRepository adminRepository;

    @Override
    public boolean checkIfExists(ObjectId id) {
        Report searchResult = reportRepository.findById(id).orElse(null);
        return (searchResult != null && searchResult.getStatus().equals("Free"));
    }

    @Override
    public boolean checkIfExistsAndInReview(ObjectId id) {
        Report searchResult = reportRepository.findById(id).orElse(null);
        return (searchResult != null && searchResult.getStatus().equals("In Review"));
    }

    @Override
    public Report findById(ObjectId id) {
        return reportRepository.findById(id).orElse(null);
    }

    @Override
    public void changeStatusToInReview(Report report) {
        reportRepository.changeStatusToInReview(report.getId());
    }

    @Override
    public List<Report> getReportsWithStatusFree() {
        return reportRepository.findByStatus("Free");
    }

    @Override
    public void deleteReportsByIdComment(ObjectId idComment, ObjectId idNotToDelete) {
        List<Report> reports = reportRepository.findByIdComment(idComment);
        List<ObjectId> ids = new ArrayList<>();
        for (Report report : reports
             ) {
            if (report.getId() != idNotToDelete) {
                ids.add(report.getId());
            }
        }
        reportRepository.deleteAllById(ids);
        adminRepository.setReportsToNull(ids);
    }

    @Override
    public void removeReportsByIds(List<ObjectId> reportIds) {
        reportRepository.deleteAllById(reportIds);
    }

}
