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
package com.ericsson.assertions.response;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ObjectAssert;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Assert methods for {@link ResponseEntity} class.
 * <p>
 * To create a new instance of this class, invoke {@link ResponseEntityAssertions#assertThat(ResponseEntity)}.
 *
 * @see ObjectAssert
 */
public class ResponseEntityAssert<T> extends AbstractAssert<ResponseEntityAssert<T>, ResponseEntity<T>> {

    protected ResponseEntityAssert(final ResponseEntity<T> actual) {
        super(actual, ResponseEntityAssert.class);
    }

    public static <T> ResponseEntityAssert<T> assertThat(final ResponseEntity<T> actual) {
        return new ResponseEntityAssert<>(actual);
    }

    /**
     * Verifies that the actual entity has a non-null body. Fails if actual is null, or actual.hasBody() == false
     *
     * @return {@link ResponseEntityAssert} assertion wrapper for {@link ResponseEntity}
     */
    public ResponseEntityAssert<T> hasBody() {
        isNotNull();
        if (!actual.hasBody()) {
            failWithMessage("Expected ResponseEntity to have a body, but it did not.");
        }
        return this;
    }

    /**
     * Verifies that the actual entity has the specified body.
     * 
     * <pre>
     * Fails if actual is null, or !actual.getBody().equals(expectedBody)
     * </pre>
     *
     * @param expectedBody
     *            the body expected in the actual ResponseEntity
     * @return {@link ResponseEntityAssert} assertion wrapper for {@link ResponseEntity}
     */
    public ResponseEntityAssert<T> hasBody(final Object expectedBody) {
        hasBody();
        if (!actual.getBody().equals(expectedBody)) {
            failWithMessage("Expected ResponseEntity to have body:%n%s%n but was:%n" +
                    "%s", expectedBody, actual.getBody());
        }
        return this;
    }

    /**
     * Verifies that the actual entity does not have non-null body. Fails if actual is null, or actual.hasBody() == true
     *
     * @return {@link ResponseEntityAssert} assertion wrapper for {@link ResponseEntity}
     */
    public ResponseEntityAssert<T> doesNotHaveBody() {
        isNotNull();
        if (actual.hasBody()) {
            failWithMessage("Expected ResponseEntity to not have a body, but it did.");
        }
        return this;
    }

    /**
     * Verifies that the actual entity has the specified HttpStatus.
     *
     * <pre>
     * Fails if actual is null, or !actual.getStatusCode().equals(expectedStatus)
     * </pre>
     *
     * @param expectedStatus
     *            the HttpStatus expected for the actual ResponseEntity
     * @return {@link ResponseEntityAssert} assertion wrapper for {@link ResponseEntity}
     * @see {@link HttpStatus}
     */
    public ResponseEntityAssert<T> hasStatus(final HttpStatus expectedStatus) {
        isNotNull();
        if (!actual.getStatusCode().equals(expectedStatus)) {
            failWithMessage("Expected ResponseEntity to have HttpStatus:%n%s%n but was:%n" +
                    "%s", expectedStatus, actual.getStatusCode());
        }
        return this;
    }

    /**
     * Verifies that the actual entity has the specified HttpStatus code value.
     *
     * <pre>
     * Fails if actual is null, or !actual.getStatusCodeValue().equals(expectedStatusCodeValue)
     * </pre>
     *
     * @param expectedStatusCodeValue
     *            the HttpStatus int value expected for the actual ResponseEntity
     * @return {@link ResponseEntityAssert} assertion wrapper for {@link ResponseEntity}
     * @see {@link HttpStatus}
     */
    public ResponseEntityAssert<T> hasStatusValue(final int expectedStatusCodeValue) {
        isNotNull();
        if (actual.getStatusCodeValue() != expectedStatusCodeValue) {
            failWithMessage("Expected ResponseEntity to have HttpStatus code:%n%s%n but was:%n" +
                    "%s", expectedStatusCodeValue, actual.getStatusCodeValue());
        }
        return this;
    }

    /**
     * Verifies that the actual entity has a String body that matches with the specified JSON according to the specified {@link JSONCompareMode}.
     *
     * <pre>
     * Fails if actual is null, or !actual.hasBody(), or !(actual.getBody() instanceOf String.class)
     * or if JSONAssert.assertEquals(expectedJson, body, compareMode) fails;
     * </pre>
     *
     * @param expectedJson
     *            the Json format String value expected for the actual ResponseEntity
     * @return {@link ResponseEntityAssert} assertion wrapper for {@link ResponseEntity}
     * @see {@link JSONAssert}
     */
    public ResponseEntityAssert<T> hasJsonBody(final String expectedJson, final JSONCompareMode compareMode) {
        hasBody();
        final Object genericBody = actual.getBody();
        if (genericBody instanceof String) {
            final String actualJson = (String) genericBody;
            try {
                JSONAssert.assertEquals(expectedJson, actualJson, compareMode);
            } catch (final JSONException e) {
                failWithMessage("Expected ResponseEntity to have Json body:%n%s%n but was:%n" +
                        "%s", expectedJson, actual.getBody());
                return this;
            }
            return this;
        } else {
            failWithMessage("Expected ResponseEntity to have Json body, but was of type:%n" +
                    "%s", genericBody.getClass());
        }

        return this;
    }

}
