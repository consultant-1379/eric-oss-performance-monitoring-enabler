/*******************************************************************************
 * COPYRIGHT Ericsson 2023 - 2024
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

package com.ericsson.oss.apps.service;

import static com.ericsson.oss.apps.util.Constants.PME_SUBSCRIPTION_NAMES;
import static com.ericsson.oss.apps.util.Constants.RAPP_ID;
import static com.ericsson.oss.apps.util.Constants.SUBSCRIPTION_STATUS_ACTIVE;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.ericsson.oss.apps.client.ddc.V1SubscriptionsApi;
import com.ericsson.oss.apps.client.ddc.model.SubscriptionGetAllRes;
import com.ericsson.oss.apps.exception.DataSubscriptionHandlingException;
import com.ericsson.oss.apps.exception.TokenAuthenticationException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataSubscriptionService {
    private final V1SubscriptionsApi dataSubscriptionApi;
    private final String rAppId;

    public DataSubscriptionService(final V1SubscriptionsApi dataSubscriptionApi,
            @Value("${gateway.services.iam.clientId}") final String rAppId) {
        this.dataSubscriptionApi = dataSubscriptionApi;
        this.rAppId = rAppId;
    }

    @Retryable(retryFor = { DataSubscriptionHandlingException.class },
            maxAttemptsExpression = "${dataSubscription.retry.get.maxAttempts}",
            backoff = @Backoff(delay = 1_000, maxDelay = 600_000, multiplier = 2))
    public void checkActiveSubscriptions() {
        final List<SubscriptionGetAllRes> subscriptions;

        log.info("Fetching data subscriptions for rApp ID '{}'", rAppId);

        try {
            subscriptions = dataSubscriptionApi.queryAllSubscriptionByParams(getSubscriptionParams());
        } catch (final ResourceAccessException | HttpServerErrorException | TokenAuthenticationException e) {
            log.error("Failed to get data subscriptions", e);
            throw new DataSubscriptionHandlingException("Failed to get data subscriptions", e);
        }

        if (subscriptions.isEmpty()) {
            log.error("PME Data Subscriptions have not been submitted for '{}'", rAppId);
            throw new DataSubscriptionHandlingException("PME Data Subscriptions have not been submitted");
        }

        final var activeSubscriptionNames = subscriptions
                .stream()
                .filter(s -> SUBSCRIPTION_STATUS_ACTIVE.equalsIgnoreCase(s.getStatus()))
                .map(SubscriptionGetAllRes::getName)
                .collect(Collectors.toSet());

        if (!activeSubscriptionNames.containsAll(PME_SUBSCRIPTION_NAMES)) {
            log.error("PME Data Subscriptions are not active for '{}'", rAppId);
            throw new DataSubscriptionHandlingException("PME Data Subscriptions are not active");
        }
        log.info("All PME Data Subscriptions are active");
    }

    private Map<String, String> getSubscriptionParams() {
        return Map.of(RAPP_ID, rAppId);
    }
}
