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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.ericsson.oss.apps.client.pmsc.KpiDefinitionApi;
import com.ericsson.oss.apps.exception.KpiDefinitionHandlingException;
import com.ericsson.oss.apps.exception.TokenAuthenticationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KpiDefinitionsService {

    private final String kpiDefinitionsFile;
    private final KpiDefinitionApi kpiDefinitionApi;
    private final ObjectMapper objectMapper;

    public KpiDefinitionsService(@Value("${kpiDefinition.fileName}") final String kpiDefinitionsFile,
                                 final KpiDefinitionApi kpiDefinitionApi, final ObjectMapper objectMapper) {
        this.kpiDefinitionsFile = kpiDefinitionsFile;
        this.kpiDefinitionApi = kpiDefinitionApi;
        this.objectMapper = objectMapper;
    }

    @Retryable(retryFor = { KpiDefinitionHandlingException.class },
            maxAttemptsExpression = "${kpiDefinition.retry.post.maxAttempts}",
            backoff = @Backoff(delay = 1_000, maxDelay = 600_000, multiplier = 2))
    public void postKpiDefinitions() {
        final JsonNode jsonPayload = readKpis();
        sendKpiDefinitions(jsonPayload);
        log.info("KPI definitions submitted successfully");
    }

    // package-private for test assertions
    JsonNode readKpis() {
        try (final InputStream inputStream = KpiDefinitionsService.class.getResourceAsStream("/" + kpiDefinitionsFile)) {
            return objectMapper.readTree(inputStream);
        } catch (final IOException e) {
            log.error("Failed to read KPI definitions file", e);
            throw new UncheckedIOException("Failed to read KPI definitions file", e);
        }
    }

    private void sendKpiDefinitions(final JsonNode jsonPayload) {
        try {
            kpiDefinitionApi.addKpiDefinitionsWithHttpInfo(jsonPayload);
        } catch (final ResourceAccessException | HttpServerErrorException | TokenAuthenticationException e) {
            log.error("Failed to send KPI definitions", e);
            throw new KpiDefinitionHandlingException("Failed to send KPI definitions", e);
        } catch (final HttpClientErrorException e) {
            if (!HttpStatus.CONFLICT.equals(e.getStatusCode())) {
                log.error("Failed to send KPI definitions", e);
                throw new KpiDefinitionHandlingException("Failed to send KPI definitions", e);
            }
            log.info("Response code from PMSCH: {}. KPI definitions have already been submitted", e.getStatusCode());
        }
    }
}
