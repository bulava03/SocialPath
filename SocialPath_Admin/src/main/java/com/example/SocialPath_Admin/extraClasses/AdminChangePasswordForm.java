package com.example.SocialPath_Admin.extraClasses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminChangePasswordForm {
    @NotBlank(message = "Логін обов'язковий.")
    @Size(max = 20, message = "Логін повинен містити не більше 20 символів.")
    private String login;
    private String passwordOld;
    @NotBlank(message = "Пароль обов'язковий.")
    @Size(min = 8, max = 20, message = "Пароль повинен містити від 8 до 20 символів.")
    private String password;
}
