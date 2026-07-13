package com.socialpath.dto.response;

/**
 * Body of a successful REST operation. The result field carries the outcome
 * the front-end switches on, e.g. "INVITED" or "ALREADY_FRIENDS".
 * @param result machine-readable outcome name
 */
public record OperationResult(String result) {

    public static OperationResult of(String result) {
        return new OperationResult(result);
    }
}
