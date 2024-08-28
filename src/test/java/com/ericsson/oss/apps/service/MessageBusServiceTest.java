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
package com.ericsson.oss.apps.service;

import com.ericsson.oss.apps.client.ddc.V1MessageBusApi;
import com.ericsson.oss.apps.client.ddc.model.MessageBusResponseDto;
import com.ericsson.oss.apps.exception.MessageBusHandlingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableRetry
@DirtiesContext
@SpringBootTest(properties = { "messageBus.retry.get.maxAttempts=3",
        "messageBus.name=" + MessageBusServiceTest.KAFKA_BOOTSTRAP_NAME }, classes = { MessageBusService.class })
@ActiveProfiles("test")
class MessageBusServiceTest {

    static final String KAFKA_BOOTSTRAP_NAME = "eric-oss-dmm-kf-op-sz-kafka-bootstrap";
    private static final String SECURE_BOOTSTRAP_ENTRY = "bootstrap.stsvp1eic28.stsoss.sero.gic.ericsson.se:443";
    private static final String IN_SECURE_BOOTSTRAP_ENTRY = "eric-oss-dmm-kf-op-sz-kafka-bootstrap:9092";

    private static final Set<String> KAFKA_ACCESS_ENDPOINTS = Set.of(IN_SECURE_BOOTSTRAP_ENTRY, SECURE_BOOTSTRAP_ENTRY);

    private static final Map<String, String> QUERY_PARAMS = Collections.emptyMap();

    @MockBean
    private V1MessageBusApi messageBusApi;

    @Autowired
    private MessageBusService objectUnderTest;

    @Test
    void whenGetMessageBusEndpoint_thenCorrectEndpointIsReturned() {
        final List<MessageBusResponseDto> mockResponse =
                List.of(createMock(KAFKA_ACCESS_ENDPOINTS));
        when(messageBusApi.getMessageBusByParam(QUERY_PARAMS)).thenReturn(mockResponse);

        final String result = objectUnderTest.getMessageBusEndpoint();
        assertThat(result).isEqualTo(SECURE_BOOTSTRAP_ENTRY);

        verify(messageBusApi, times(1)).getMessageBusByParam(QUERY_PARAMS);
    }

    @Test
    void whenGetMessageBusEndpointAndNoSecureEntry_thenExceptionIsThrown() {
        final List<MessageBusResponseDto> mockResponse =
                List.of(createMock(Set.of(IN_SECURE_BOOTSTRAP_ENTRY)));
        when(messageBusApi.getMessageBusByParam(QUERY_PARAMS)).thenReturn(mockResponse);

        assertThatThrownBy(() -> objectUnderTest.getMessageBusEndpoint())
                .isInstanceOf(MessageBusHandlingException.class)
                .hasMessage("Unable to find secure message bus endpoint");

        verify(messageBusApi, times(3)).getMessageBusByParam(QUERY_PARAMS);
    }

    @Test
    void whenGetMessageBusEndpointAndNoKafkaBootstrapEntry_thenExceptionIsThrown() {
        when(messageBusApi.getMessageBusByParam(QUERY_PARAMS)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> objectUnderTest.getMessageBusEndpoint())
                .isInstanceOf(MessageBusHandlingException.class)
                .hasMessage("No message bus endpoints available");

        verify(messageBusApi, times(3)).getMessageBusByParam(QUERY_PARAMS);
    }

    @Test
    void whenGetMessageBusEndpointAndExceptionOccurs_thenExceptionIsThrown() {
        when(messageBusApi.getMessageBusByParam(QUERY_PARAMS))
                .thenThrow(ResourceAccessException.class)
                .thenThrow(HttpServerErrorException.class);

        assertThatThrownBy(() -> objectUnderTest.getMessageBusEndpoint())
                .isInstanceOf(MessageBusHandlingException.class)
                .hasMessage("Failed to get message bus endpoint")
                .hasCauseInstanceOf(HttpServerErrorException.class);

        verify(messageBusApi, times(3)).getMessageBusByParam(QUERY_PARAMS);
    }

    private static MessageBusResponseDto createMock(final Set<String> accessEndPoints) {
        final MessageBusResponseDto messageBusResponseDto = mock(MessageBusResponseDto.class);
        when(messageBusResponseDto.getName()).thenReturn(KAFKA_BOOTSTRAP_NAME);
        when(messageBusResponseDto.getAccessEndpoints()).thenReturn(accessEndPoints);
        return messageBusResponseDto;
    }
}