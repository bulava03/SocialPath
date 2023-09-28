package com.example.SocialPath.extraClasses;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GroupCreationForm {
    private String owner;
    private String ownerPassword;

    @NotEmpty(message = "Назва групи не може бути порожньою")
    @Size(max = 50, message = "Довжина назви групи повинна бути не більше 50 символів")
    private String name;

    private List<String> members;
    private List<String> admins;

    public GroupCreationForm(String owner, String ownerPassword) {
        this.owner = owner;
        this.ownerPassword = ownerPassword;
    }

    public GroupCreationForm(String owner, String ownerPassword, String name) {
        this.owner = owner;
        this.ownerPassword = ownerPassword;
        this.name = name;
    }
}
