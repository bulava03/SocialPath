package com.socialpath;

import com.socialpath.dto.request.ChangePasswordForm;
import com.socialpath.entity.User;
import com.socialpath.security.AuthCookie;
import com.socialpath.service.HandleAvatarService;
import com.socialpath.service.PhoneVerificationService;
import com.socialpath.sms.DemoSmsGateway;
import com.socialpath.service.SessionService;
import com.socialpath.service.UserService;
import com.socialpath.web.SecurityController;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The password change: every reason to refuse it, and what a successful one does. */
class SecurityControllerTest {

    private UserService userService;
    private SessionService sessionService;
    private SecurityController controller;
    private final User anna = new User();

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        sessionService = mock(SessionService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        controller = new SecurityController(userService, sessionService, mock(HandleAvatarService.class),
                mock(AuthCookie.class), passwordEncoder, mock(PhoneVerificationService.class),
                new DemoSmsGateway(false));
        anna.setLogin("anna");
        anna.setPassword("hash");
        when(passwordEncoder.matches("current-pass", "hash")).thenReturn(true);
    }

    /** Validates the form the way Spring MVC does for a @Valid parameter. */
    private static BindingResult validate(ChangePasswordForm form) {
        BindingResult result = new BeanPropertyBindingResult(form, "form");
        new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator()).validate(form, result);
        return result;
    }

    private static ChangePasswordForm form(String oldPassword, String password, String confirmPassword) {
        ChangePasswordForm form = new ChangePasswordForm();
        form.setOldPassword(oldPassword);
        form.setPassword(password);
        form.setConfirmPassword(confirmPassword);
        return form;
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "current-pass | short          | short          | Password must be between 8 and 20 characters long.",
        "current-pass | brand-new-pass | brand-new-pas  | The new passwords do not match.",
        "wrong-pass   | brand-new-pass | brand-new-pass | The old password is incorrect.",
    })
    void changePassword_Refused_ShowsWhyAndChangesNothing(String oldPassword, String password,
                                                          String confirmPassword, String reason) {
        ChangePasswordForm form = form(oldPassword, password, confirmPassword);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.changePassword(anna, "session-1", form, validate(form), model);

        assertEquals("user/security", view);
        assertEquals(reason, model.get("errorText"));
        verify(userService, never()).updatePassword(anyString(), anyString());
        verify(sessionService, never()).endOthers(any(), any());
    }

    @Test
    void changePassword_Accepted_KeepsOnlyThisSession() {
        ChangePasswordForm form = form("current-pass", "brand-new-pass", "brand-new-pass");

        String view = controller.changePassword(anna, "session-1", form, validate(form), new ExtendedModelMap());

        assertEquals("redirect:/security/password?passwordChanged", view);
        verify(userService).updatePassword("anna", "brand-new-pass");
        verify(sessionService).endOthers("anna", "session-1");
    }
}
