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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Map;

import com.ericsson.oss.apps.exception.TokenDecoderException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class JwtDecoder {
    private static final String USER_NAME_CLAIM = "preferred_username";

    private static String decodeJWTToken(final String token) throws UnsupportedEncodingException {
        final Base64.Decoder decoder = Base64.getUrlDecoder();

        final String[] chunks = token.split("\\.");

        return new String(decoder.decode(chunks[1]), "UTF-8");
    }

    public static String getUserName(final String token) {
        final String payload;
        try {
            payload = decodeJWTToken(token);
        } catch (final UnsupportedEncodingException e) {
            throw new TokenDecoderException("Failed to decode token", e);
        }
        final ObjectMapper mapper = new ObjectMapper();
        final Map<String, String> map;
        try {
            map = mapper.readValue(payload, Map.class);
        } catch (final IOException e) {
            throw new TokenDecoderException("Failed to get userName from token", e);
        }
        return map.get(USER_NAME_CLAIM);
    }
}
