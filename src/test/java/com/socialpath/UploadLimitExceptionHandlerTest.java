package com.socialpath;

import com.socialpath.exception.UploadLimitExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadLimitExceptionHandlerTest {

    private static final String BROWSER_ACCEPT = "text/html,application/xhtml+xml,*/*;q=0.8";

    private final UploadLimitExceptionHandler handler = new UploadLimitExceptionHandler();
    private MockHttpServletRequest request;
    private FlashMap flash;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("POST", "/user/changeUserInfo");
        // The DispatcherServlet provides this map for every request.
        flash = new FlashMap();
        request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, flash);
    }

    @Test
    void formSubmission_GoesBackToTheFormWithTheMessage() {
        // Arrange
        request.addHeader("Accept", BROWSER_ACCEPT);
        request.addHeader("Referer", "http://localhost:8080/user/getUserInfo");

        // Act
        ModelAndView result = handler.handleTooLarge(request);

        // Assert
        assertEquals("redirect:/user/getUserInfo", result.getViewName());
        assertEquals(UploadLimitExceptionHandler.MESSAGE, flash.get("errorText"));
    }

    @Test
    void formSubmission_KeepsTheQueryOfThePage() {
        // Arrange: the group form is opened with ?id=...
        request.addHeader("Accept", BROWSER_ACCEPT);
        request.addHeader("Referer", "http://localhost:8080/group/getGroupUpdatingForm?id=42");

        // Act
        ModelAndView result = handler.handleTooLarge(request);

        // Assert
        assertEquals("redirect:/group/getGroupUpdatingForm?id=42", result.getViewName());
    }

    @Test
    void formSubmission_NeverRedirectsToAnotherSite() {
        // Arrange: only the path of the Referer is used; "//host" would leave the site
        request.addHeader("Accept", BROWSER_ACCEPT);
        request.addHeader("Referer", "http://localhost:8080//evil.example/phish");

        // Act
        ModelAndView result = handler.handleTooLarge(request);

        // Assert
        assertEquals("redirect:/", result.getViewName());
    }

    @Test
    void formSubmission_WithoutReferer_GoesToTheStartPage() {
        // Arrange
        request.addHeader("Accept", BROWSER_ACCEPT);

        // Act
        ModelAndView result = handler.handleTooLarge(request);

        // Assert
        assertEquals("redirect:/", result.getViewName());
    }

    @Test
    void fetchRequest_GetsJsonError413() {
        // Arrange: fetch() sends "Accept: */*"
        request.addHeader("Accept", "*/*");

        // Act
        ModelAndView result = handler.handleTooLarge(request);

        // Assert
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, result.getStatus());
        assertInstanceOf(MappingJackson2JsonView.class, result.getView());
        assertEquals(UploadLimitExceptionHandler.MESSAGE, result.getModel().get("error"));
        assertTrue(flash.isEmpty());
    }
}
