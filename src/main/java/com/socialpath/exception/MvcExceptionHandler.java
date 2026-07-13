package com.socialpath.exception;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Page-rendering controllers previously re-checked ban/existence in every
 * method and returned either a redirect or `home/index` with an errorText.
 * That logic now lives here: a banned or missing account lands back on the
 * home page carrying the message, exactly as before but in a single place.
 *
 * REST controllers are handled by RestExceptionHandler, which is registered
 * with higher precedence, so for a @RestController that advice wins and this
 * one never renders a view for a JSON endpoint.
 */
@ControllerAdvice(annotations = Controller.class)
public class MvcExceptionHandler {

    @ExceptionHandler(UserBannedException.class)
    public String handleBanned(UserBannedException ex, Model model) {
        model.addAttribute("errorText", ex.getMessage());
        return "home/index";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound() {
        return "redirect:/";
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public String handleForbidden() {
        return "redirect:/";
    }
}
