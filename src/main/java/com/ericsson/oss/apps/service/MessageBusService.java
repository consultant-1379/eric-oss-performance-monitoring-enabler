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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.ericsson.oss.apps.client.ddc.V1MessageBusApi;
import com.ericsson.oss.apps.client.ddc.model.MessageBusResponseDto;
import com.ericsson.oss.apps.exception.MessageBusHandlingException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MessageBusService {

    private final V1MessageBusApi messageBusApi;

    private final String name;

    public MessageBusService(final V1MessageBusApi messageBusApi, @Value("${messageBus.name}") final String name) {
        this.messageBusApi = messageBusApi;
        this.name = name;
    }

    @Retryable(retryFor = { MessageBusHandlingException.class },
            maxAttemptsExpression = "${messageBus.retry.get.maxAttempts}",
            backoff = @Backoff(delay = 1_000, maxDelay = 600_000, multiplier = 2))
    public String getMessageBusEndpoint() {
        final List<MessageBusResponseDto> messageBusResponse;

        try {
            messageBusResponse = messageBusApi.getMessageBusByParam(Collections.emptyMap());
        } catch (final ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to get message bus endpoint", e);
            throw new MessageBusHandlingException("Failed to get message bus endpoint", e);
        }

        if (messageBusResponse.isEmpty()) {
            log.error("No message bus endpoints available for '{}'", name);
            throw new MessageBusHandlingException("No message bus endpoints available");
        }

        final Optional<String> messageBusEndPoint = messageBusResponse
                .stream()
                .filter(messageBusResponseDto -> messageBusResponseDto.getName().equalsIgnoreCase(name))
                .map(MessageBusResponseDto::getAccessEndpoints)
                .flatMap(Collection::stream)
                .filter(accessEndPoint -> accessEndPoint.endsWith("443"))
                .findFirst();

        return messageBusEndPoint.orElseThrow(() -> new MessageBusHandlingException("Unable to find secure message bus endpoint"));
    }
}
