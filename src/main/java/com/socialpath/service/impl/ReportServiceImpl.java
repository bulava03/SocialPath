package com.socialpath.service.impl;

import lombok.RequiredArgsConstructor;
import com.socialpath.document.Report;
import com.socialpath.repository.ReportRepository;
import com.socialpath.service.ReportService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;

    @Override
    public void addReport(Report report) {
        reportRepository.save(report);
    }
}
