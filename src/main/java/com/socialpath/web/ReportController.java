package com.socialpath.web;

import com.socialpath.document.Report;
import com.socialpath.dto.request.LeftFrameRequest;
import com.socialpath.dto.request.NewReport;
import com.socialpath.dto.response.UserLogin;
import com.socialpath.helper.ReportHelper;
import com.socialpath.security.SecurityUtils;
import com.socialpath.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Set;

@Controller
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private static final Set<String> ALLOWED_TYPES = Set.of("Comment User", "Comment Group", "Group");

    private final ReportService reportService;

    /**
     * Stores a report and sends the user back to the page they reported from
     * (post/redirect/get, so a refresh does not resubmit the report).
     * @param newReport report details from the form
     * @return redirect to the originating page
     */
    @PostMapping("/addReport")
    public String addReport(@ModelAttribute("newReport") NewReport newReport) {
        String login = SecurityUtils.getCurrentLogin();

        // The reporter is whoever is authenticated, not whatever the form claims.
        newReport.setAuthorLogin(login);

        if (!ALLOWED_TYPES.contains(newReport.getType())) {
            return "redirect:/user/authorisation";
        }

        Report report = ReportHelper.ConvertNewReportToReport(newReport);
        reportService.addReport(report);

        if (newReport.getType().equals("Comment User")) {
            if (newReport.getIdPage().equals(login)) {
                return "redirect:/user/authorisation";
            }
            return "redirect:/user/anotherUserPage?anotherUserLogin=" + newReport.getIdPage();
        }
        return "redirect:/searchResult/getGroupPage?groupId=" + newReport.getIdPage();
    }

    @GetMapping("/getReportPage")
    public String getReportPage(@ModelAttribute("request") LeftFrameRequest request, Model model) {
        model.addAttribute("id", request.getId());
        model.addAttribute("author", new UserLogin(SecurityUtils.getCurrentLogin(), ""));
        return "report/reportPage";
    }
}
