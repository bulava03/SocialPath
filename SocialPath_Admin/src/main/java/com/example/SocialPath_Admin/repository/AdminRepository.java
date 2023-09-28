package com.example.SocialPath_Admin.repository;

import com.example.SocialPath_Admin.document.Admin;
import org.bson.types.ObjectId;

import java.util.List;

public interface AdminRepository {
    Admin findAdminByLoginAndPassword(String login, String password);
    void updatePassword(Admin admin);
    void addReport(Admin admin);
    void setReportToNull(String adminId);
    void updateAdminReportsToNull(List<String> reportIds);
    void setReportsToNull(List<ObjectId> ids);
}
