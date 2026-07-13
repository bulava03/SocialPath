package com.socialpath.exception;

import com.socialpath.dto.response.ApiError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain exceptions from REST endpoints (/friends, /comments,
 * /groupMembership, /auth) to HTTP statuses with a JSON error body.
 *
 * Highest precedence so that for a request handled by a @RestController this
 * advice is consulted before the page-rendering MvcExceptionHandler.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.socialpath.web.rest")
public class RestExceptionHandler {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ApiError handleNotFound(ResourceNotFoundException ex) {
        return new ApiError(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler({ForbiddenOperationException.class, UserBannedException.class})
    public ApiError handleForbidden(RuntimeException ex) {
        return new ApiError(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiError handleBadRequest(IllegalArgumentException ex) {
        return new ApiError(ex.getMessage());
    }
}
