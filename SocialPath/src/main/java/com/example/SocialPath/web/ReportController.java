package com.example.SocialPath.web;

import com.example.SocialPath.document.Report;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.LeftFrameRequest;
import com.example.SocialPath.extraClasses.NewReport;
import com.example.SocialPath.extraClasses.UserLogin;
import com.example.SocialPath.helper.CheckHelper;
import com.example.SocialPath.helper.ReportHelper;
import com.example.SocialPath.service.*;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private UserService userService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private CommentsService commentsService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ModelAttributesService modelAttributesService;

    @PostMapping("/addReport")
    public String addReport(@ModelAttribute("newReport") NewReport newReport, Model model) {
        User myUser = userService.findUserByLoginAndPassword(newReport.getAuthorLogin(), newReport.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).equals("")) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        }

        if (!newReport.getType().equals("Comment User") && !newReport.getType().equals("Comment Group") && !newReport.getType().equals("Group")) {
            model = modelAttributesService.usersAttributes(model, myUser, true, commentsService.loadComments("User", myUser.getLogin()));
            return "user/userPage";
        }

        Report report = ReportHelper.ConvertNewReportToReport(newReport);
        reportService.addReport(report);

        if (newReport.getType().equals("Comment User")) {
            User anotherUser = userService.findUserById(newReport.getIdPage());
            anotherUser.setPassword("");

            model.addAttribute("user", anotherUser);
            model.addAttribute("author", new UserLogin(newReport.getAuthorLogin(), newReport.getAuthorPassword()));
            model.addAttribute("isAuthor", newReport.getIdPage().equals(newReport.getAuthorLogin()));
            model.addAttribute("InRequests", CheckHelper.inRequestsCheck(anotherUser, myUser, newReport.getAuthorLogin(), newReport.getIdPage()));
            model.addAttribute("publications", commentsService.loadComments("User", newReport.getIdPage()));
            return "user/userPage";
        } else {
            model.addAttribute("group", groupService.findGroupById(new ObjectId(newReport.getIdPage())));
            model.addAttribute("author", userService.findUserByLoginAndPassword(newReport.getAuthorLogin(), newReport.getAuthorPassword()));
            model.addAttribute("publications", commentsService.loadComments("Group", newReport.getIdPage()));
            return "group/groupPage";
        }
    }

    @GetMapping("/getReportPage")
    public String GetReportPage(@ModelAttribute("request") LeftFrameRequest request, Model model) {
        model.addAttribute("id", request.getId());
        model.addAttribute("author", new UserLogin(request.getAuthorLogin(), request.getAuthorPassword()));
        return "report/reportPage";
    }

}
