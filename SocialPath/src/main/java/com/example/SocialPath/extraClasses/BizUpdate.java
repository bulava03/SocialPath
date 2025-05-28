package com.example.SocialPath.extraClasses;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BizUpdate {
    private String login;

    @NotBlank(message = "Пароль обов'язковий.")
    @Size(min = 8, max = 20, message = "Пароль повинен містити від 8 до 20 символів.")
    private String password;

    private String imageId;

    @NotEmpty(message = "Назва групи не може бути порожньою")
    @Size(max = 50, message = "Довжина назви групи повинна бути не більше 50 символів")
    private String name;

    @Size(max = 50, message = "Довжина гасла повинна бути не більше 50 символів")
    private String slogan;

    @NotBlank(message = "Електронна пошта обов'язкова.")
    @Email(message = "Некоректна адреса електронної пошти.")
    @Size(max = 100, message = "Адреса електронної пошти повинна містити не більше 100 символів.")
    private String email;

    @NotBlank(message = "Номер телефону обов'язковий")
    @Pattern(regexp = "^[\\d\\s()+-]+$", message = "Номер телефону може містити лише цифри, пробіли та символи '+', '-', '(', ')'.")
    @Size(max = 20, message = "Номер телефону повинен містити не більше 20 символів.")
    private String phoneNumber;

    private String concreteAddress;
    private double latitude;
    private double longitude;
    private boolean onlyOnline;

    private List<String> jobs;
}
