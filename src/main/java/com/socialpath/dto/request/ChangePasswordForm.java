package com.socialpath.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordForm {

    @NotBlank(message = "{user.password.required}")
    private String oldPassword;

    @NotBlank(message = "{user.password.required}")
    @Size(min = 8, max = 20, message = "{user.password.size}")
    private String password;

    /** The new password typed a second time, to catch a typo before it locks the user out. */
    @NotBlank(message = "{user.password.confirm.required}")
    private String confirmPassword;
}
