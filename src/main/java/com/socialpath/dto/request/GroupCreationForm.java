package com.socialpath.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GroupCreationForm {
    private String owner;

    @NotEmpty(message = "{group.name.required}")
    @Size(max = 50, message = "{group.name.size}")
    private String name;

    private List<String> members;
    private List<String> admins;

    public GroupCreationForm(String owner) {
        this.owner = owner;
    }
}
