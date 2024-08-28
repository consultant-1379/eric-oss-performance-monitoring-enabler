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

public class TokenAuthenticationException extends RuntimeException {
    private static final long serialVersionUID = -4867062760165263419L;

    public TokenAuthenticationException(final String message) {
        super(message);
    }

    public TokenAuthenticationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
