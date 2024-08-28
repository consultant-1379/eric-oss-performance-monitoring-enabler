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

package com.ericsson.assertions;

import org.assertj.core.api.Assertions;
import org.springframework.http.ResponseEntity;

import com.ericsson.assertions.response.ResponseEntityAssert;

/**
 * Implementation of {@link Assertions} for use with {@link ResponseEntityAssert}.
 *
 * @see Assertions
 */
public final class EpmeAssertions extends Assertions {
    private EpmeAssertions() {
        super();
    }

    public static <T> ResponseEntityAssert<T> assertThat(final ResponseEntity<T> actual) {
        return ResponseEntityAssert.assertThat(actual);
    }
}
