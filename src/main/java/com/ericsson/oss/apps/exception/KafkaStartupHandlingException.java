/*******************************************************************************
 * COPYRIGHT Ericsson 2024
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

public class KafkaStartupHandlingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 2137287599950785898L;

    public KafkaStartupHandlingException(final String message) {
        super(message);
    }

    public KafkaStartupHandlingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
