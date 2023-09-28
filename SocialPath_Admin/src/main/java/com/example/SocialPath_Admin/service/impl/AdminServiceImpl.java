package com.example.SocialPath_Admin.service.impl;

import com.example.SocialPath_Admin.document.Admin;
import com.example.SocialPath_Admin.document.Report;
import com.example.SocialPath_Admin.extraClasses.AdminChangePasswordForm;
import com.example.SocialPath_Admin.repository.AdminRepository;
import com.example.SocialPath_Admin.service.AdminService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class AdminServiceImpl implements AdminService {
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private Validator validator;

    public Object[] validateAdmin(AdminChangePasswordForm admin) {
        Set<ConstraintViolation<AdminChangePasswordForm>> violations = validator.validate(admin);

        if (!violations.isEmpty()) {
            List<String> errorMessages = violations.stream()
                    .map(ConstraintViolation::getMessage).toList();
            return new Object[] { false, errorMessages.stream().findFirst() };
        } else {
            return new Object[] { true, "" };
        }
    }

    public Admin findByLoginAndPassword(String id, String password) {
        return adminRepository.findAdminByLoginAndPassword(id, password);
    }

    public void updatePassword(Admin admin) {
        adminRepository.updatePassword(admin);
    }

    public void addReport(Admin admin, Report report) {
        admin.setReport(report.getId());
        adminRepository.addReport(admin);
    }

    public void setReportToNull(String adminId) {
        adminRepository.setReportToNull(adminId);
    }

    public void updateAdminReportsToNull(List<String> reportIds) {
        adminRepository.updateAdminReportsToNull(reportIds);
    }
}
