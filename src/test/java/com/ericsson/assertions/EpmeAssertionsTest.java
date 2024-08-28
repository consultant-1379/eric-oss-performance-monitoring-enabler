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

import static com.ericsson.oss.apps.util.Constants.EMPTY_STRING;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit test class for {@link EpmeAssertions} and {@link EpmeSoftAssertions} classes.
 */
@ExtendWith(SoftAssertionsExtension.class)
public class EpmeAssertionsTest {
    private static final String STRING_BODY = "body";
    private static final ResponseEntity<String> STRING_OK_ENTITY = ResponseEntity.ok(STRING_BODY);

    @InjectSoftAssertions
    private EpmeSoftAssertions softly;

    @Test
    void testAllAssertionsCanPass() {
        EpmeAssertions.assertThat(STRING_OK_ENTITY)
                .hasBody()
                .hasBody(STRING_BODY)
                .hasStatus(HttpStatus.OK)
                .hasStatusValue(200);
        softly.assertThat(STRING_OK_ENTITY)
                .hasBody()
                .hasBody(STRING_BODY)
                .hasStatus(HttpStatus.OK)
                .hasStatusValue(200);
    }

    @Test
    void testResponseEntityAssertionCanFail() {
        assertThatCode(() -> EpmeAssertions.assertThat(STRING_OK_ENTITY).hasBody(EMPTY_STRING)).isInstanceOf(AssertionError.class);
    }
}
