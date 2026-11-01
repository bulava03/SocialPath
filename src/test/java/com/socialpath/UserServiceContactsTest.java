package com.socialpath;

import com.socialpath.dto.request.UserUpdate;
import com.socialpath.entity.ContactVisibility;
import com.socialpath.entity.User;
import com.socialpath.repository.GroupRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.CountryService;
import com.socialpath.service.UserService;
import com.socialpath.service.impl.UserServiceImpl;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Saving the profile's contacts: a new number waits for its code, a new
 * address loses its confirmation, and who sees a contact can be chosen only
 * once it is confirmed.
 */
class UserServiceContactsTest {

    private static final String PHONE = "+12015550123";

    private UserRepository userRepository;
    private UserServiceImpl service;
    private User anna;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        CountryService countries = mock(CountryService.class);
        when(countries.isValidCode(null)).thenReturn(true);
        service = new UserServiceImpl(userRepository, mock(GroupRepository.class),
                Validation.buildDefaultValidatorFactory().getValidator(), mock(PasswordEncoder.class), countries);

        anna = new User();
        anna.setLogin("anna");
        anna.setFirstName("Anna");
        anna.setEmail("anna@example.com");
        anna.setPhone(PHONE);
        anna.setPhoneVerified(true);
        anna.setEmailVerified(true);
        when(userRepository.findByLogin("anna")).thenReturn(anna);
    }

    /** The form as the profile page sends it, unchanged. */
    private UserUpdate form() {
        UserUpdate form = UserUpdate.from(anna);
        form.setPhone(PHONE);
        return form;
    }

    @Test
    void updateUser_NewNumber_IsNotSavedUntilItsCodeIsEntered() {
        UserUpdate form = form();
        form.setPhone("+12015550199");

        service.updateUser(form);

        assertEquals(PHONE, anna.getPhone());
        assertTrue(anna.isPhoneVerified());
    }

    @Test
    void updateUser_NewAddress_IsSavedUnconfirmed() {
        UserUpdate form = form();
        form.setEmail(" new@example.com ");

        service.updateUser(form);

        assertEquals("new@example.com", anna.getEmail());
        assertFalse(anna.isEmailVerified());
    }

    @Test
    void updateUser_SameAddressInAnotherCase_StaysConfirmed() {
        UserUpdate form = form();
        form.setEmail("Anna@Example.com");

        service.updateUser(form);

        assertTrue(anna.isEmailVerified());
    }

    @Test
    void updateUser_Visibility_IsSavedForConfirmedContactsOnly() {
        anna.setEmailVerified(false);
        UserUpdate form = form();
        form.setPhoneVisibility(ContactVisibility.EVERYONE);
        form.setEmailVisibility(ContactVisibility.EVERYONE);

        service.updateUser(form);

        assertEquals(ContactVisibility.EVERYONE, anna.getPhoneVisibility());
        assertEquals(ContactVisibility.FRIENDS, anna.getEmailVisibility());
    }

    @Test
    void updateUser_VisibilityLeftOutByTheForm_KeepsTheStoredOne() {
        anna.setPhoneVisibility(ContactVisibility.NOBODY);
        UserUpdate form = form();
        form.setPhoneVisibility(null);

        service.updateUser(form);

        assertEquals(ContactVisibility.NOBODY, anna.getPhoneVisibility());
    }

    @Test
    void validateUserUpdate_AddressConfirmedByAnotherAccount_IsRefused() {
        UserUpdate form = form();
        form.setPhoneRegion("US");
        form.setPhoneNumber("201 555 0123");
        form.setEmail("taken@example.com");
        when(userRepository.isEmailVerifiedByOther("taken@example.com", "anna")).thenReturn(true);

        assertEquals(UserService.EMAIL_TAKEN, service.validateUserUpdate(form).getMessage());
    }
}
