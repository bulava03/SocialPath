package com.example.SocialPath_SuperAdmin.service.impl;

import com.example.SocialPath_SuperAdmin.repository.ReportRepository;
import com.example.SocialPath_SuperAdmin.service.ReportService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private ReportRepository reportRepository;

    @Override
    public void setReportFree(String id) {
        reportRepository.updateStatusById(new ObjectId(id), "Free");
    }
}
