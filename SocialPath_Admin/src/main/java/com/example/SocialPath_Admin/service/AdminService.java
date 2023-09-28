package com.example.SocialPath_Admin.service;

import com.example.SocialPath_Admin.document.Admin;
import com.example.SocialPath_Admin.document.Report;
import com.example.SocialPath_Admin.extraClasses.AdminChangePasswordForm;

import java.util.List;

public interface AdminService {
    Object[] validateAdmin(AdminChangePasswordForm admin);
    Admin findByLoginAndPassword(String id, String password);
    void updatePassword(Admin admin);
    void addReport(Admin admin, Report report);
    void setReportToNull(String adminId);
    void updateAdminReportsToNull(List<String> reportIds);
}
