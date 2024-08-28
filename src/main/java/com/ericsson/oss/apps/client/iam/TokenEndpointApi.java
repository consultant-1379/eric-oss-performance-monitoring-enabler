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

package com.ericsson.oss.apps.client.iam;

import static com.ericsson.oss.apps.util.Constants.CLIENT_CREDENTIALS;
import static com.ericsson.oss.apps.util.Constants.CLIENT_ID;
import static com.ericsson.oss.apps.util.Constants.CLIENT_SECRET;
import static com.ericsson.oss.apps.util.Constants.GRANT_TYPE;

import java.util.HashMap;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

import com.ericsson.oss.apps.client.ApiClient;
import com.ericsson.oss.apps.client.iam.model.AccessTokenResponse;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TokenEndpointApi {
    private static final String[] EMPTY_STRING_ARRAY = new String[] {};
    private static final ParameterizedTypeReference<AccessTokenResponse> RETURN_TYPE = new ParameterizedTypeReference<>() {
    };
    private final ApiClient apiClient;
    private final String tokenEndpointPath;

    public AccessTokenResponse getToken(final String clientId, final String clientSecret) throws RestClientException {
        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();

        formParams.add(GRANT_TYPE, CLIENT_CREDENTIALS);
        formParams.add(CLIENT_ID, clientId);
        formParams.add(CLIENT_SECRET, clientSecret);

        return apiClient.invokeAPI(tokenEndpointPath, HttpMethod.POST, new HashMap<>(), queryParams, null,
                headerParams, cookieParams, formParams, List.of(MediaType.APPLICATION_JSON), MediaType.APPLICATION_FORM_URLENCODED,
                EMPTY_STRING_ARRAY,
                RETURN_TYPE).getBody();
    }
}
