package com.example.SocialPath_SuperAdmin.web.rest;

import com.example.SocialPath_SuperAdmin.other.ValidationResult;
import org.springframework.ui.Model;
import com.example.SocialPath_SuperAdmin.document.Admin;
import com.example.SocialPath_SuperAdmin.service.AdminService;
import com.example.SocialPath_SuperAdmin.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping()
public class AdminController {
    @Autowired
    private AdminService adminService;
    @Autowired
    private ReportService reportService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("admins", adminService.getAllAdmins());
        return "admin/index";
    }

    @GetMapping("/create")
    public String createAdminForm(Model model) {
        model.addAttribute("admin", new Admin());
        return "admin/createAdmin";
    }

    @PostMapping("/create")
    public String createAdmin(@ModelAttribute("admin") Admin admin, Model model) {
        ValidationResult validation = adminService.validateAdmin(admin);
        if (!validation.isSuccess()) {
            model.addAttribute("admin", admin);
            model.addAttribute("errorText", validation.getMessage());
            return "admin/createAdmin";
        } else {
            adminService.addAdmin(admin);
            model.addAttribute("admins", adminService.getAllAdmins());
            return "admin/index";
        }
    }

    @GetMapping("/update")
    public String updateAdminForm(@ModelAttribute("admin") Admin adm, Model model) {
        Admin admin = adminService.findById(adm.getLogin());
        model.addAttribute("admin", admin);
        return "admin/adminInfo";
    }

    @PostMapping("/update")
    public String updateAdmin(@ModelAttribute("admin") Admin admin, Model model) {
        ValidationResult validation = adminService.validateUpdateAdmin(admin);
        if (!validation.isSuccess()) {
            model.addAttribute("admin", admin);
            model.addAttribute("errorText", validation.getMessage());
            return "admin/adminInfo";
        } else {
            adminService.changeAdmin(admin);
            model.addAttribute("admins", adminService.getAllAdmins());
            return "admin/index";
        }
    }

    @PostMapping("/delete")
    public String deleteAdmin(@ModelAttribute("admin") Admin adm, Model model) {
        Admin admin = adminService.findById(adm.getLogin());
        if (admin.getReport() != null && !admin.getReport().isEmpty()) {
            reportService.setReportFree(admin.getReport());
        }
        adminService.removeAdmin(admin);
        model.addAttribute("admins", adminService.getAllAdmins());
        return "admin/index";
    }
}
