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

package com.ericsson.oss.apps.service;

import static com.ericsson.oss.apps.util.Constants.PME_SUBSCRIPTION_NAMES;
import static com.ericsson.oss.apps.util.Constants.RAPP_ID;
import static com.ericsson.oss.apps.util.Constants.SUBSCRIPTION_STATUS_ACTIVE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.ericsson.oss.apps.client.ddc.V1SubscriptionsApi;
import com.ericsson.oss.apps.client.ddc.model.SubscriptionGetAllRes;
import com.ericsson.oss.apps.exception.DataSubscriptionHandlingException;
import com.ericsson.oss.apps.exception.TokenAuthenticationException;
import com.fasterxml.jackson.databind.ObjectMapper;

@EnableRetry
@DirtiesContext
@SpringBootTest(properties = { "dataSubscription.retry.get.maxAttempts=3",
        "gateway.services.iam.clientId=" + DataSubscriptionServiceTest.CLIENT_ID }, classes = { DataSubscriptionService.class })
@ActiveProfiles("test")
class DataSubscriptionServiceTest {
    static final String CLIENT_ID = "pme-test-client";
    private static final Map<String, String> QUERY_PARAMS = Map.of(RAPP_ID, CLIENT_ID);
    private static final List<SubscriptionGetAllRes> PME_MOCK_SUBSCRIPTIONS = PME_SUBSCRIPTION_NAMES.stream()
            .map(name -> {
                final var subscription = mock(SubscriptionGetAllRes.class);
                when(subscription.getName()).thenReturn(name);
                when(subscription.getStatus()).thenReturn(SUBSCRIPTION_STATUS_ACTIVE);
                return subscription;
            }).toList();

    @SpyBean
    private ObjectMapper objectMapper;
    @MockBean
    private V1SubscriptionsApi subscriptionApi;
    @Autowired
    private DataSubscriptionService objectUnderTest;

    @Test
    void whenCheckActiveSubscriptions_thenQueryAllSubscriptionsIsCalled() {
        when(subscriptionApi.queryAllSubscriptionByParams(QUERY_PARAMS)).thenReturn(PME_MOCK_SUBSCRIPTIONS);

        objectUnderTest.checkActiveSubscriptions();

        verify(subscriptionApi, times(1)).queryAllSubscriptionByParams(QUERY_PARAMS);
    }

    @Test
    void whenCheckActiveSubscriptions_andNoSubscriptionsReturnedFirstAttempts_thenNoExceptionIsThrown() {
        when(subscriptionApi.queryAllSubscriptionByParams(QUERY_PARAMS))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList())
                .thenReturn(PME_MOCK_SUBSCRIPTIONS);

        objectUnderTest.checkActiveSubscriptions();
        verify(subscriptionApi, times(3)).queryAllSubscriptionByParams(QUERY_PARAMS);
    }

    @Test
    void whenCheckActiveSubscriptions_andNoSubscriptionsReturned_thenExceptionIsThrown() {
        when(subscriptionApi.queryAllSubscriptionByParams(QUERY_PARAMS)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> objectUnderTest.checkActiveSubscriptions())
                .isInstanceOf(DataSubscriptionHandlingException.class)
                .hasMessage("PME Data Subscriptions have not been submitted")
                .hasNoCause();

        verify(subscriptionApi, times(3)).queryAllSubscriptionByParams(QUERY_PARAMS);
    }

    @Test
    void whenCheckActiveSubscriptions_andNoActiveSubscriptionsReturned_thenExceptionIsThrown() {
        final var inactiveSubscriptions = PME_MOCK_SUBSCRIPTIONS.stream().peek(sub -> when(sub.getStatus()).thenReturn("Inactive")).toList();
        when(subscriptionApi.queryAllSubscriptionByParams(QUERY_PARAMS)).thenReturn(inactiveSubscriptions);

        assertThatThrownBy(() -> objectUnderTest.checkActiveSubscriptions())
                .isInstanceOf(DataSubscriptionHandlingException.class)
                .hasMessage("PME Data Subscriptions are not active")
                .hasNoCause();

        verify(subscriptionApi, times(3)).queryAllSubscriptionByParams(QUERY_PARAMS);
    }

    @Test
    void whenCheckActiveSubscriptions_andRuntimeExceptionOccurs_thenCheckActiveSubscriptionsIsNotRetried() {
        when(subscriptionApi.queryAllSubscriptionByParams(QUERY_PARAMS)).thenThrow(RuntimeException.class);
        assertThatThrownBy(() -> objectUnderTest.checkActiveSubscriptions())
                .isInstanceOf(RuntimeException.class);

        verify(subscriptionApi, times(1)).queryAllSubscriptionByParams(QUERY_PARAMS);
    }

    @Test
    void whenCheckActiveSubscriptions_andExceptionOccursMaxAttempts_thenExceptionIsThrown() {
        when(subscriptionApi.queryAllSubscriptionByParams(QUERY_PARAMS))
                .thenThrow(ResourceAccessException.class)
                .thenThrow(TokenAuthenticationException.class)
                .thenThrow(HttpServerErrorException.class);

        assertThatThrownBy(() -> objectUnderTest.checkActiveSubscriptions())
                .isInstanceOf(DataSubscriptionHandlingException.class)
                .hasMessage("Failed to get data subscriptions")
                .hasCauseInstanceOf(HttpServerErrorException.class);

        verify(subscriptionApi, times(3)).queryAllSubscriptionByParams(QUERY_PARAMS);
    }
}