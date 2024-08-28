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

package com.ericsson.oss.apps.util;

import static com.ericsson.oss.apps.util.TestConstants.TOKEN;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;

import com.ericsson.oss.apps.exception.TokenDecoderException;

public class JwtDecoderTest {
    public static final String BAD_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI3YXY0YWlHUC0t" +
            "R2swRnRIYnlJa3lXUVJTUjBhcW9peWRVbWpIWENpd1VrIn0.abcd";

    @Test
    public void verifyDecodeJWTToken() {
        final String userName = JwtDecoder.getUserName(TOKEN);
        assertThat(userName).isEqualTo("pme-configuration-operator");
    }

    @Test
    public void verifyExceptionThrown_whenInvalidToken() {
        assertThatCode(() -> JwtDecoder.getUserName(BAD_TOKEN))
                .isInstanceOf(TokenDecoderException.class);
    }
}
