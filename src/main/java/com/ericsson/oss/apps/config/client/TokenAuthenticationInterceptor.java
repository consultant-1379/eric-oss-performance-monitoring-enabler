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

package com.ericsson.oss.apps.config.client;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import com.ericsson.oss.apps.client.iam.TokenEndpointApi;
import com.ericsson.oss.apps.client.iam.model.AccessTokenResponse;
import com.ericsson.oss.apps.exception.TokenAuthenticationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnExpression("!('${gateway.services.iam.clientId}'.isBlank() || '${gateway.services.iam.clientSecret}'.isBlank())")
public class TokenAuthenticationInterceptor implements ClientHttpRequestInterceptor {
    private static final String BEARER_TEMPLATE = "Bearer %s";
    private static final int TOKEN_LIFESPAN_BUFFER = 30;

    private final String clientId;
    private final String clientSecret;

    private final TokenEndpointApi tokenEndpointApi;

    private final AtomicReference<AccessTokenResponse> tokenResponse = new AtomicReference<>();
    private final AtomicReference<ZonedDateTime> tokenLastUpdated = new AtomicReference<>();

    public TokenAuthenticationInterceptor(final TokenEndpointApi tokenEndpointApi,
            @Value("${gateway.services.iam.clientId}") final String clientId,
            @Value("${gateway.services.iam.clientSecret}") final String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenEndpointApi = tokenEndpointApi;
    }

    @Override
    public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
            final ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().add(HttpHeaders.AUTHORIZATION, String.format(BEARER_TEMPLATE, getAccessToken()));
        return execution.execute(request, body);
    }

    private String getAccessToken() {
        synchronized (tokenResponse) {
            if (shouldRefreshToken()) {
                log.debug("Access token is expired, refreshing for new one");
                refreshToken();
            }
        }

        if (Objects.isNull(tokenResponse.get())) {
            return null;
        }
        return tokenResponse.get().getAccessToken();
    }

    private boolean shouldRefreshToken() {
        if (Objects.isNull(tokenResponse.get()) || Objects.isNull(tokenLastUpdated.get())) {
            return true;
        }
        final int tokenLifespan = tokenResponse.get().getExpiresIn();
        final ZonedDateTime lastUpdated = tokenLastUpdated.get();
        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        return (ChronoUnit.SECONDS.between(lastUpdated, now)) > tokenLifespan - TOKEN_LIFESPAN_BUFFER;
    }

    private void refreshToken() {
        try {
            final AccessTokenResponse response = tokenEndpointApi.getToken(clientId, clientSecret);
            if (Objects.isNull(response)) {
                resetToken();
                log.error("Failed to get access token. Request was successful but returned empty body");
                throw new TokenAuthenticationException("Failed to get access token. Request was successful but returned empty body");
            }

            tokenResponse.set(response);
            tokenLastUpdated.set(ZonedDateTime.now(ZoneOffset.UTC));
        } catch (final RestClientException e) {
            resetToken();
            log.error("Failed to get access token", e);
            throw new TokenAuthenticationException("Failed to get access token", e);
        }
    }

    void resetToken() {
        tokenResponse.set(null);
        tokenLastUpdated.set(null);
    }
}