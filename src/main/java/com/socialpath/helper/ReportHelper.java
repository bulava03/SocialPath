package com.socialpath.helper;

import com.socialpath.entity.Report;
import com.socialpath.dto.request.NewReport;

public class ReportHelper {

    public static Report ConvertNewReportToReport(NewReport newReport)
    {
        if (newReport.getType().equals("Comment User")) {
            Report report = new Report(
                    newReport.getAuthorLogin(),
                    newReport.getType(),
                    newReport.getSubject(),
                    newReport.getIdPublication(),
                    newReport.getIdComment()
            );

            if (report.getIdPublication().equals("publications")) {
                report.setIdUser(newReport.getIdPage());
            }

            return report;
        } else if (newReport.getType().equals("Comment Group")) {
            Report report = new Report(
                    newReport.getAuthorLogin(),
                    newReport.getType(),
                    newReport.getSubject(),
                    newReport.getIdPublication(),
                    newReport.getIdComment()
            );

            if (report.getIdPublication().equals("publications")) {
                report.setIdGroup(newReport.getIdPage());
            }

            return report;
        } else {
            return new Report(
                    newReport.getAuthorLogin(),
                    newReport.getType(),
                    newReport.getSubject(),
                    newReport.getIdPage()
            );
        }
    }

}
