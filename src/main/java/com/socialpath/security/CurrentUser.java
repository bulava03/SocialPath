package com.socialpath.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the signed-in {@link com.socialpath.entity.User} into a controller
 * method. The JWT filter has already loaded that user to authenticate the
 * request (see {@link SecurityUser}), so controllers reuse it instead of
 * looking it up again.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "user")
public @interface CurrentUser {
}
