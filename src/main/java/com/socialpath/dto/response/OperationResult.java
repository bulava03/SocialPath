package com.socialpath.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Body of a successful REST operation. The result field carries the outcome
 * the front-end switches on, e.g. "INVITED" or "ALREADY_FRIENDS".
 * @param result machine-readable outcome name
 * @param id the id of what the operation created, if anything
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OperationResult(String result, Long id) {

    public static OperationResult of(String result) {
        return new OperationResult(result, null);
    }

    public static OperationResult created(Long id) {
        return new OperationResult("OK", id);
    }
}
