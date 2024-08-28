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

public class DataSubscriptionHandlingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 6680720058251204166L;

    public DataSubscriptionHandlingException(final String message) {
        super(message);
    }

    public DataSubscriptionHandlingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
