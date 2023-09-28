package com.example.SocialPath_Admin.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "admin")
public class Admin {
    @Id
    @NotBlank(message = "Логін обов'язковий.")
    @Size(max = 20, message = "Логін повинен містити не більше 20 символів.")
    private String login;

    @NotBlank(message = "Пароль обов'язковий.")
    @Size(min = 8, max = 20, message = "Пароль повинен містити від 8 до 20 символів.")
    private String password;

    private ObjectId report;
}
