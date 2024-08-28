/*******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.apps.exception;

import java.io.Serial;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

public class ControllerDetailException extends ResponseStatusException {
    @Serial
    private static final long serialVersionUID = -6107865627359913155L;

    @Getter
    private final String detail;

    public ControllerDetailException(final HttpStatusCode status, final String reason, final String detail) {
        super(status, reason);
        this.detail = detail;
    }

    public ControllerDetailException(final HttpStatusCode status, final String reason) {
        this(status, reason, null);
    }

    public HttpStatusCode getStatus() {
        return super.getStatusCode();
    }

    public static Builder builder() {
        return new Builder();
    }

    @With(AccessLevel.PUBLIC)
    @AllArgsConstructor
    public static class Builder {
        private HttpStatus status = HttpStatus.OK;
        private String reason;
        private String detail;

        private Builder() {
        }

        public ControllerDetailException build() {
            return new ControllerDetailException(status, reason, detail);
        }
    }
}
