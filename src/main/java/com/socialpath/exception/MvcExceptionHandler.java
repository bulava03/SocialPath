package com.socialpath.exception;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

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
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(Model model) {
        return errorPage(model, HttpStatus.NOT_FOUND, "This page does not exist or has been deleted.");
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleForbidden(Model model) {
        return errorPage(model, HttpStatus.FORBIDDEN, "You are not allowed to do this.");
    }

    private String errorPage(Model model, HttpStatus status, String message) {
        model.addAttribute("status", status.value());
        model.addAttribute("message", message);
        return "error";
    }
}
