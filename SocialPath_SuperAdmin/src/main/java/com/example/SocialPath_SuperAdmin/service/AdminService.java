package com.example.SocialPath_SuperAdmin.service;

import com.example.SocialPath_SuperAdmin.document.Admin;
import com.example.SocialPath_SuperAdmin.other.ValidationResult;

import java.util.List;

public interface AdminService {
    ValidationResult validateAdmin(Admin admin);

    ValidationResult validateUpdateAdmin(Admin admin);

    Admin findById(String login);

    void addAdmin(Admin admin);

    void removeAdmin(Admin admin);

    void changeAdmin(Admin admin);

    List<Admin> getAllAdmins();
}
