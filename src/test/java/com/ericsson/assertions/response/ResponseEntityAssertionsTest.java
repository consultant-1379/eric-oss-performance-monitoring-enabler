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

import static com.ericsson.oss.apps.util.Constants.EMPTY_STRING;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;

import java.util.Optional;

import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit test class for {@link ResponseEntityAssertions} and {@link ResponseEntitySoftAssertions} classes.
 */
@ExtendWith(SoftAssertionsExtension.class)
class ResponseEntityAssertionsTest {

    private static final String STRING_BODY = "string";
    private static final String JSON_BODY = """
            {
                "json": [
                    { "id": "2", "arrayA": [ { "id": "2" }, { "id": "1", "arrayB": { } } ] }
                ]
            }
            """;
    private static final String EQUIVALENT_JSON_BODY = formatJson(JSON_BODY);
    private static final String INCORRECT_JSON = """
            {
                "other": [ "id": "2" ]
            }
            """;
    private static final String INVALID_JSON = "}}{";
    private static final long LONG_BODY = 1L;
    private static final ResponseEntity<String> STRING_OK_ENTITY = ResponseEntity.ok(STRING_BODY);
    private static final ResponseEntity<String> JSON_CORRECT_ENTITY = ResponseEntity.ok(JSON_BODY);
    private static final ResponseEntity<String> JSON_EQUIVALENT_ENTITY = ResponseEntity.ok(EQUIVALENT_JSON_BODY);
    private static final ResponseEntity<String> JSON_INCORRECT_ENTITY = ResponseEntity.ok(INCORRECT_JSON);
    private static final ResponseEntity<String> JSON_INVALID_ENTITY = ResponseEntity.ok(INVALID_JSON);
    private static final ResponseEntity<Long> LONG_BAD_ENTITY = ResponseEntity.badRequest().body(LONG_BODY);
    private static final ResponseEntity<Double> EMPTY_404_ENTITY = ResponseEntity.of(Optional.empty());
    private static final ResponseEntity<Integer> NULL_ENTITY = null;

    @InjectSoftAssertions
    private ResponseEntitySoftAssertions softly;

    @Test
    void testAllAssertionsCanPass() {
        ResponseEntityAssertions.assertThat(STRING_OK_ENTITY)
                .hasBody()
                .hasBody(STRING_BODY)
                .hasStatus(HttpStatus.OK)
                .hasStatusValue(200);
        ResponseEntityAssertions.assertThat(JSON_CORRECT_ENTITY)
                .hasBody()
                .hasBody(JSON_BODY)
                .hasStatus(HttpStatus.OK)
                .hasStatusValue(200)
                .hasJsonBody(JSON_BODY, JSONCompareMode.LENIENT)
                .hasJsonBody(EQUIVALENT_JSON_BODY, JSONCompareMode.LENIENT);
        ResponseEntityAssertions.assertThat(JSON_EQUIVALENT_ENTITY)
                .hasBody()
                .hasBody(EQUIVALENT_JSON_BODY)
                .hasStatus(HttpStatus.OK)
                .hasStatusValue(200)
                .hasJsonBody(EQUIVALENT_JSON_BODY, JSONCompareMode.LENIENT)
                .hasJsonBody(JSON_BODY, JSONCompareMode.LENIENT);
        ResponseEntityAssertions.assertThat(LONG_BAD_ENTITY)
                .hasBody()
                .hasBody(LONG_BODY)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasStatusValue(HttpStatus.BAD_REQUEST.value());
        ResponseEntityAssertions.assertThat(EMPTY_404_ENTITY)
                .doesNotHaveBody()
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasStatusValue(HttpStatus.NOT_FOUND.value());
        softly.assertThat(STRING_OK_ENTITY)
                .hasBody()
                .hasBody(STRING_BODY)
                .hasStatus(HttpStatus.OK)
                .hasStatusValue(HttpStatus.OK.value());
        softly.assertThat(LONG_BAD_ENTITY)
                .hasBody()
                .hasBody(LONG_BODY)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasStatusValue(HttpStatus.BAD_REQUEST.value());
        softly.assertThat(EMPTY_404_ENTITY)
                .doesNotHaveBody()
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasStatusValue(404);
    }

    @Test
    void testHasAnyBodyCanFail() {
        assertThatCode(() -> ResponseEntityAssertions.assertThat(EMPTY_404_ENTITY).hasBody()).isInstanceOf(AssertionError.class);
    }

    @Test
    void testHasSpecificBodyCanFail() {
        assertThatCode(() -> ResponseEntityAssertions.assertThat(EMPTY_404_ENTITY).hasBody(0.0D)).isInstanceOf(AssertionError.class);
    }

    @Test
    void testHasJsonBodyCanFail() {
        softly.assertThatCode(() -> ResponseEntityAssertions.assertThat(JSON_CORRECT_ENTITY).hasJsonBody(EMPTY_STRING, JSONCompareMode.LENIENT))
                .isInstanceOf(AssertionError.class);
        softly.assertThatCode(() -> ResponseEntityAssertions.assertThat(JSON_CORRECT_ENTITY).hasJsonBody(INVALID_JSON, JSONCompareMode.LENIENT))
                .isInstanceOf(AssertionError.class);
        softly.assertThatCode(() -> ResponseEntityAssertions.assertThat(JSON_CORRECT_ENTITY).hasJsonBody(INCORRECT_JSON, JSONCompareMode.LENIENT))
                .isInstanceOf(AssertionError.class);
        softly.assertThatCode(() -> ResponseEntityAssertions.assertThat(JSON_INCORRECT_ENTITY).hasJsonBody(JSON_BODY, JSONCompareMode.LENIENT))
                .isInstanceOf(AssertionError.class);
        softly.assertThatCode(() -> ResponseEntityAssertions.assertThat(JSON_INVALID_ENTITY).hasJsonBody(JSON_BODY, JSONCompareMode.LENIENT))
                .isInstanceOf(AssertionError.class);
        softly.assertThatCode(
                () -> ResponseEntityAssertions.assertThat(ResponseEntity.ok(EMPTY_STRING)).hasJsonBody(JSON_BODY, JSONCompareMode.LENIENT))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void testHasJsonBodyCanFailIfNoBody() {
        assertThatCode(() -> ResponseEntityAssertions.assertThat(EMPTY_404_ENTITY).hasJsonBody(EMPTY_STRING, JSONCompareMode.LENIENT))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void testHasJsonBodyCanFailIfNonStringBody() {
        assertThatCode(() -> ResponseEntityAssertions.assertThat(LONG_BAD_ENTITY).hasJsonBody(EMPTY_STRING, JSONCompareMode.LENIENT))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void testHasStatusCanFail() {
        assertThatCode(() -> ResponseEntityAssertions.assertThat(EMPTY_404_ENTITY).hasStatus(HttpStatus.OK)).isInstanceOf(AssertionError.class);
    }

    @Test
    void testHasStatusValueCanFail() {
        assertThatCode(() -> ResponseEntityAssertions.assertThat(EMPTY_404_ENTITY).hasStatusValue(200)).isInstanceOf(AssertionError.class);
    }

    @Test
    void testAssertionCanFailIfActualIsNull() {
        assertThatCode(() -> ResponseEntityAssertions.assertThat(NULL_ENTITY).doesNotHaveBody())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expecting actual not to be null");
    }

    private static String formatJson(final String rawJson) {
        final var objectMapper = new ObjectMapper();
        try {
            final var jsonObject = objectMapper.readValue(rawJson, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        } catch (final JsonProcessingException e) {
            fail("TEST SETUP FAILURE", e);
        }
        return null;
    }
}
