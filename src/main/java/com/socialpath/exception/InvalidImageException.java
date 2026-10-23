package com.socialpath.exception;

/**
 * An uploaded image that cannot be used as an avatar: an unsupported format,
 * a damaged file, or one too large to decode safely. The message is meant to
 * be shown to the user.
 */
public class InvalidImageException extends RuntimeException {
    public InvalidImageException(String message) {
        super(message);
    }

    public InvalidImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
