package com.example.SocialPath.document;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import java.util.List;

@Data
@Document(collection = "user")
public class User {
    @MongoId
    @NotBlank(message = "Логін обов'язковий.")
    @Size(max = 20, message = "Логін повинен містити не більше 20 символів.")
    private String login;

    @NotBlank(message = "Пароль обов'язковий.")
    @Size(min = 8, max = 20, message = "Пароль повинен містити від 8 до 20 символів.")
    private String password;

    private String imageId;

    @NotBlank(message = "Ім'я обов'язкове.")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Ім'я може містити тільки літери.")
    @Size(max = 50, message = "Ім'я повинне містити не більше 50 символів.")
    private String firstName;

    @Size(max = 50, message = "Прізвище повинне містити не більше 50 символів.")
    private String lastName;

    @NotBlank(message = "Електронна пошта обов'язкова.")
    @Email(message = "Некоректна адреса електронної пошти.")
    @Size(max = 100, message = "Адреса електронної пошти повинна містити не більше 100 символів.")
    private String email;

    @NotBlank(message = "Номер телефону обов'язковий")
    @Pattern(regexp = "^[\\d\\s()+-]+$", message = "Номер телефону може містити лише цифри, пробіли та символи '+', '-', '(', ')'.")
    @Size(max = 20, message = "Номер телефону повинен містити не більше 20 символів.")
    private String phoneNumber;

    @NotNull(message = "Дата народження обов'язкова.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateOfBirth;

    private String country;
    private String region;
    private String city;
    private String education;
    private String workplace;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime ban;
    private List<ObjectId> groups;
    private List<String> friends;
    private List<String> friendInvites;
    private List<ObjectId> publications;
}
