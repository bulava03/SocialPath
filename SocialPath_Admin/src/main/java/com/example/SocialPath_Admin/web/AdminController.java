package com.example.SocialPath_Admin.web;

import com.example.SocialPath_Admin.document.Admin;
import com.example.SocialPath_Admin.document.Group;
import com.example.SocialPath_Admin.document.Publication;
import com.example.SocialPath_Admin.document.Report;
import com.example.SocialPath_Admin.extraClasses.AdminChangePasswordForm;
import com.example.SocialPath_Admin.extraClasses.ReportQuery;
import com.example.SocialPath_Admin.service.*;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private CommentsService commentsService;
    @Autowired
    private UserService userService;

    @GetMapping("/authorisation")
    public String authorisation(@ModelAttribute("admin") Admin admin, Model model) {
        admin = adminService.findByLoginAndPassword(admin.getLogin(), admin.getPassword()); ;
        if (admin == null) {
            model.addAttribute("errorText", "Помилка авторизації");
            return "home/index";
        }
        model.addAttribute("errorText", "");
        model.addAttribute("admin", admin);
        model.addAttribute("reports", reportService.getReportsWithStatusFree());
        if (admin.getReport() != null) {
            model.addAttribute("report", reportService.findById(admin.getReport()));
        }
        return "admin/adminPage";
    }

    @GetMapping("/getChangePasswordForm")
    public String getChangePasswordForm(@ModelAttribute("admin") AdminChangePasswordForm admin, Model model) {
        Admin adminOld = adminService.findByLoginAndPassword(admin.getLogin(), admin.getPasswordOld());
        if (adminOld == null) {
            model.addAttribute("errorText", "Виникла помилка");
            return "home/index";
        }
        model.addAttribute("errorText", "");
        model.addAttribute("admin", admin);
        return "admin/adminInfo";
    }

    @PostMapping("/getChangePasswordForm")
    public String changePassword(@ModelAttribute("admin") AdminChangePasswordForm admin, Model model) {
        Admin adminOld = adminService.findByLoginAndPassword(admin.getLogin(), admin.getPasswordOld());
        if (adminOld == null) {
            model.addAttribute("errorText", "Виникла помилка");
            return "home/index";
        }

        Object[] validation = adminService.validateAdmin(admin);

        if (!(boolean) validation[0]) {
            model.addAttribute("errorText", validation[1].toString().replaceAll("Optional\\[|\\]", ""));
            model.addAttribute("admin", admin);
            return "admin/adminInfo";
        }

        Admin adminUpdated = adminService.findByLoginAndPassword(admin.getLogin(), admin.getPasswordOld());
        adminUpdated.setPassword(admin.getPassword());
        adminService.updatePassword(adminUpdated);

        Admin adminExists = adminService.findByLoginAndPassword(adminUpdated.getLogin(), adminUpdated.getPassword());

        model.addAttribute("errorText", "");
        model.addAttribute("admin", adminExists);
        model.addAttribute("reports", reportService.getReportsWithStatusFree());
        if (adminExists.getReport() != null) {
            model.addAttribute("report", reportService.findById(adminExists.getReport()));
        }
        return "admin/adminPage";
    }

    @PostMapping("/getReport")
    public String getReport(@ModelAttribute("admin") Admin admin, Model model) {
        Admin adminExists = adminService.findByLoginAndPassword(admin.getLogin(), admin.getPassword());
        if (adminExists == null) {
            model.addAttribute("errorText", "Виникла помилка");
            return "home/index";
        }

        if (reportService.checkIfExists(admin.getReport())) {
            Report report = reportService.findById(admin.getReport());
            if (adminExists.getReport() == null) {
                reportService.changeStatusToInReview(report);
                adminService.addReport(admin, report);

                model.addAttribute("errorText", "");
                model.addAttribute("admin", admin);
                model.addAttribute("reports", reportService.getReportsWithStatusFree());
                if (admin.getReport() != null) {
                    model.addAttribute("report", reportService.findById(admin.getReport()));
                }
                return "admin/adminPage";
            } else {
                model.addAttribute("errorText", "У вас вже є скарга на розгляді");
                model.addAttribute("admin", admin);
                model.addAttribute("reports", reportService.getReportsWithStatusFree());
                if (admin.getReport() != null) {
                    model.addAttribute("report", reportService.findById(admin.getReport()));
                }
                return "admin/adminPage";
            }
        } else {
            model.addAttribute("errorText", "Виникла помилка");
            model.addAttribute("admin", admin);
            model.addAttribute("reports", reportService.getReportsWithStatusFree());
            if (admin.getReport() != null) {
                model.addAttribute("report", reportService.findById(admin.getReport()));
            }
            return "admin/adminPage";
        }
    }

    @GetMapping("/getDecisionPage")
    public String getDecisionPage(@ModelAttribute("admin") Admin admin, Model model) {
        Admin adminExists = adminService.findByLoginAndPassword(admin.getLogin(), admin.getPassword());
        if (adminExists == null) {
            model.addAttribute("errorText", "Виникла помилка");
            return "home/index";
        }

        if (admin.getReport() != null && reportService.checkIfExistsAndInReview(admin.getReport())) {
            Report report = reportService.findById(admin.getReport());

            if (report.getType().equals("Group")) {
                Group group = groupService.findById(report.getIdGroup());
                model.addAttribute("publications", commentsService.loadComments(report.getIdGroup()));
                model.addAttribute("group", group);
            } else {
                model.addAttribute("publication", commentsService.findById(report.getIdComment()));
            }

            model.addAttribute("report", report);
            model.addAttribute("admin", admin);
            return "admin/reportDecision";
        } else {
            model.addAttribute("errorText", "Виникла помилка");
            model.addAttribute("admin", admin);
            model.addAttribute("reports", reportService.getReportsWithStatusFree());
            if (admin.getReport() != null) {
                model.addAttribute("report", reportService.findById(admin.getReport()));
            }
            return "admin/adminPage";
        }
    }

    @PostMapping("/setReportResult")
    public String setReportResult(@ModelAttribute("reportQuery") ReportQuery reportQuery, Model model) {
        Admin admin = adminService.findByLoginAndPassword(reportQuery.getLogin(), reportQuery.getPassword());
        if (admin == null) {
            model.addAttribute("errorText", "Виникла помилка");
            return "home/index";
        }

        Report report = reportService.findById(reportQuery.getId());

        if (reportService.checkIfExistsAndInReview(report.getId()) && admin.getReport().equals(report.getId())) {
            if (report.getType().equals("Comment User")) {
                if (reportQuery.getResult().equals("Ban")) {
                    Publication publication = commentsService.findById(report.getIdComment());
                    String userLogin = publication.getAuthorId();
                    userService.updateBanById(userLogin);
                    if (commentsService.findById(report.getIdComment()) != null) {
                        if (report.getIdPublication().equals("publications")) {
                            commentsService.removePublicationUser(report.getIdUser(), report.getIdComment());
                        } else {
                            commentsService.removeCommentUser(new ObjectId(report.getIdPublication()), report.getIdComment());
                        }
                    }
                    reportService.deleteReportsByIdComment(report.getIdComment(), report.getId());
                }
            }
            else if (report.getType().equals("Comment Group")) {
                if (reportQuery.getResult().equals("Ban")) {
                    Publication publication = commentsService.findById(report.getIdComment());
                    String userLogin = publication.getAuthorId();
                    userService.updateBanById(userLogin);
                    if (commentsService.findById(report.getIdComment()) != null) {
                        if (report.getIdPublication().equals("publications")) {
                            commentsService.removePublicationGroup(report.getIdGroup(), report.getIdComment());
                        } else {
                            commentsService.removeCommentGroup(new ObjectId(report.getIdPublication()), report.getIdComment());
                        }
                    }
                    reportService.deleteReportsByIdComment(report.getIdComment(), report.getId());
                }
            }
            else {
                if (reportQuery.getResult().equals("Delete")) {
                    userService.removeGroupFromAllUsers(report.getIdGroup());
                    Group group = groupService.findById(report.getIdGroup());
                    if (group.getPublications() != null) {
                        if (group.getPublications().size() > 0) {
                            for (ObjectId publication : group.getPublications()
                            ) {
                                commentsService.removePublicationGroup(report.getIdGroup(), publication);
                            }
                        }
                    }
                    groupService.removeGroupById(report.getIdGroup());
                    reportService.deleteReportsByIdComment(report.getIdComment(), report.getId());
                }
            }
            reportService.deleteReportsByIdComment(report.getIdComment(), report.getId());
            adminService.setReportToNull(reportQuery.getLogin());
        }
        else {
            model.addAttribute("errorText", "Виникла помилка");
        }
        model.addAttribute("admin", admin);
        model.addAttribute("reports", reportService.getReportsWithStatusFree());
        if (admin.getReport() != null) {
            model.addAttribute("report", reportService.findById(admin.getReport()));
        }
        return "admin/adminPage";
    }
}
