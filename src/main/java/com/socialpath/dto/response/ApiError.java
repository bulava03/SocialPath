package com.socialpath.dto.response;

/**
 * Body of a failed REST operation.
 * @param error human-readable description of what went wrong
 */
public record ApiError(String error) {
}
