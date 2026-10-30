package com.socialpath.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import java.net.URI;
import java.util.Map;

/**
 * An upload over the multipart limits (application.properties) fails while the
 * request is being read, before any controller is chosen. MvcExceptionHandler
 * and RestExceptionHandler only apply to their own controllers, so without
 * this advice the user would land on the generic error page.
 *
 * A form submission (the browser asks for HTML: profile and group avatars)
 * goes back to the page it came from, with the message shown at the top of
 * the form. A fetch request (posts and comments) gets a 413 with the same JSON
 * error body the REST endpoints use.
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UploadLimitExceptionHandler {

    /** Shown to the user; the limit matches spring.servlet.multipart.max-file-size. */
    public static final String MESSAGE = "The file is too large. The limit is 100 MB per file.";

    /**
     * Turns an oversized upload into a message on the form, or a JSON error.
     * @param request the rejected request
     * @return a redirect back to the form, or a 413 JSON error
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleTooLarge(HttpServletRequest request) {
        if (!acceptsHtml(request)) {
            ModelAndView json = new ModelAndView(new MappingJackson2JsonView(), Map.of("error", MESSAGE));
            json.setStatus(HttpStatus.PAYLOAD_TOO_LARGE);
            return json;
        }
        RequestContextUtils.getOutputFlashMap(request).put("errorText", MESSAGE);
        return new ModelAndView("redirect:" + pageToReturnTo(request));
    }

    private static boolean acceptsHtml(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept != null && accept.contains(MediaType.TEXT_HTML_VALUE);
    }

    /**
     * The path (and query) of the page the form was on, from the Referer
     * header. Only the path is used, never the host, so the redirect always
     * stays on this site.
     * @param request the rejected request
     * @return a site-relative path, "/" when it cannot be determined
     */
    private static String pageToReturnTo(HttpServletRequest request) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null) {
            return "/";
        }
        try {
            URI uri = URI.create(referer);
            String path = uri.getRawPath();
            // "//host" would be read by the browser as another site.
            if (path == null || !path.startsWith("/") || path.startsWith("//")) {
                return "/";
            }
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }
}
