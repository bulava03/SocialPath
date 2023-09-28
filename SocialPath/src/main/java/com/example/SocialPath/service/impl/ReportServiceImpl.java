package com.example.SocialPath.service.impl;

import com.example.SocialPath.document.Report;
import com.example.SocialPath.repository.ReportRepository;
import com.example.SocialPath.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private ReportRepository reportRepository;

    @Override
    public void addReport(Report report) {
        reportRepository.save(report);
    }
}
