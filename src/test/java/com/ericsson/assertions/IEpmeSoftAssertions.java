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

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.SoftAssertionsProvider;
import org.assertj.core.util.CheckReturnValue;
import org.springframework.http.ResponseEntity;

import com.ericsson.assertions.response.ResponseEntityAssert;

/**
 * Implementation of SoftAssertions for {@link EpmeAssertions}. Note: Use with
 *
 * <pre>
 * &#64;ExtendWith(SoftAssertionsExtension.class)
 * &#64;InjectSoftAssertions
 * private EpmeSoftAssertions softly; //no need to call {@link SoftAssertions#assertAll()} explicitly
 * </pre>
 *
 * @see SoftAssertions
 */
interface IEpmeSoftAssertions extends SoftAssertionsProvider {
    @CheckReturnValue
    default <T> ResponseEntityAssert<T> assertThat(final ResponseEntity<T> actual) {
        return proxy(ResponseEntityAssert.class, ResponseEntity.class, actual);
    }
}
