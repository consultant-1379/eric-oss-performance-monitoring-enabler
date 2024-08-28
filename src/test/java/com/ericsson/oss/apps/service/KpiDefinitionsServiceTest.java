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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.apache.commons.collections4.IteratorUtils;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.ericsson.oss.apps.client.pmsc.KpiDefinitionApi;
import com.ericsson.oss.apps.exception.KpiDefinitionHandlingException;
import com.ericsson.oss.apps.exception.TokenAuthenticationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@EnableRetry
@DirtiesContext
@SpringBootTest(properties = { "kpiDefinition.retry.post.maxAttempts=3" }, classes = { KpiDefinitionsService.class })
@ActiveProfiles("test")
class KpiDefinitionsServiceTest {
    @SpyBean
    private ObjectMapper objectMapper;
    @MockBean
    private KpiDefinitionApi kpiDefinitionApi;
    @Autowired
    private KpiDefinitionsService objectUnderTest;

    @Test
    void whenReadJsonFile_verifyKpiTablesAndKpiCount() {
        final var kpiNodes = objectUnderTest.readKpis();
        assertThat(kpiNodes).hasSize(2);
        assertThat(kpiNodes.has("scheduled_simple")).isTrue();
        assertKpiDefinitionNode(kpiNodes.get("scheduled_simple"), "scheduled_simple",
                tuple("pme_cell_simple_fdd", 35),
                tuple("pme_cell_simple_tdd", 35),
                tuple("pme_cell_simple_nrcellcu", 17),
                tuple("pme_cell_simple_nrcelldu", 8));
        assertThat(kpiNodes.has("scheduled_complex")).isTrue();
        assertKpiDefinitionNode(kpiNodes.get("scheduled_complex"), "scheduled_complex", tuple("pme_cell_complex", 36));
    }

    @Test
    void whenObjectMapperFailsToParseFile_thenUncheckedIoExceptionIsThrown() throws IOException {
        doThrow(new IOException()).when(objectMapper).readTree(any(InputStream.class));
        assertThatThrownBy(() -> objectUnderTest.readKpis())
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseInstanceOf(IOException.class)
                .hasMessage("Failed to read KPI definitions file");
        verify(kpiDefinitionApi, never()).addKpiDefinitionsWithHttpInfo(any(JsonNode.class));
    }

    @Test
    void whenPostKpiDefinitions_thenKpiDefinitionApiIsCalled() {
        objectUnderTest.postKpiDefinitions();
        verify(kpiDefinitionApi, times(1)).addKpiDefinitionsWithHttpInfo(any(JsonNode.class));
    }

    @Test
    void whenPostKpiDefinitionsAndConflictThrown_thenKpiDefinitionHandleExceptionIsNotThrown() {
        final var httpClientException = mock(HttpClientErrorException.class);
        when(httpClientException.getStatusCode()).thenReturn(HttpStatus.CONFLICT);
        when(kpiDefinitionApi.addKpiDefinitionsWithHttpInfo(any(JsonNode.class))).thenThrow(httpClientException);

        objectUnderTest.postKpiDefinitions();

        verify(kpiDefinitionApi, times(1)).addKpiDefinitionsWithHttpInfo(any(JsonNode.class));
    }

    @Test
    void whenPostKpiDefinitions_andRuntimeExceptionOccurs_thenPostKpiDefinitionsIsNotRetried() {
        when(kpiDefinitionApi.addKpiDefinitionsWithHttpInfo(any(JsonNode.class)))
                .thenThrow(RuntimeException.class);

        assertThatThrownBy(() -> objectUnderTest.postKpiDefinitions())
                .isInstanceOf(RuntimeException.class);

        verify(kpiDefinitionApi, times(1)).addKpiDefinitionsWithHttpInfo(any(JsonNode.class));
    }

    @Test
    void whenPostKpiDefinitions_andExceptionOccursMaxAttempts_thenKpiDefinitionHandleExceptionIsThrown() {
        when(kpiDefinitionApi.addKpiDefinitionsWithHttpInfo(any(JsonNode.class)))
                .thenThrow(ResourceAccessException.class)
                .thenThrow(TokenAuthenticationException.class)
                .thenThrow(HttpClientErrorException.class);

        assertThatThrownBy(() -> objectUnderTest.postKpiDefinitions())
                .isInstanceOf(KpiDefinitionHandlingException.class)
                .hasCauseInstanceOf(HttpClientErrorException.class)
                .hasMessage("Failed to send KPI definitions");

        verify(kpiDefinitionApi, times(3)).addKpiDefinitionsWithHttpInfo(any(JsonNode.class));
    }

    @Test
    void whenPostKpiDefinitionsAndClientExceptionThrown_thenKpiDefinitionHandleExceptionIsThrown() {
        final var httpClientException = mock(HttpClientErrorException.class);
        when(httpClientException.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(kpiDefinitionApi.addKpiDefinitionsWithHttpInfo(any(JsonNode.class))).thenThrow(httpClientException);

        assertThatThrownBy(() -> objectUnderTest.postKpiDefinitions())
                .isInstanceOf(KpiDefinitionHandlingException.class)
                .hasCauseInstanceOf(HttpClientErrorException.class)
                .hasMessage("Failed to send KPI definitions");

        verify(kpiDefinitionApi, times(3)).addKpiDefinitionsWithHttpInfo(any(JsonNode.class));
    }

    @Test
    void whenPostKpiDefinitionsAndResourceAccessExceptionThrown_thenKpiDefinitionHandleExceptionIsThrown() {
        when(kpiDefinitionApi.addKpiDefinitionsWithHttpInfo(any(JsonNode.class))).thenThrow(ResourceAccessException.class);

        assertThatThrownBy(() -> objectUnderTest.postKpiDefinitions())
                .isInstanceOf(KpiDefinitionHandlingException.class)
                .hasCauseInstanceOf(ResourceAccessException.class)
                .hasMessage("Failed to send KPI definitions");

        verify(kpiDefinitionApi, times(3)).addKpiDefinitionsWithHttpInfo(any(JsonNode.class));
    }

    @Test
    void whenPostKpiDefinitionsAndTokenAuthenticationExceptionExceptionThrown_thenKpiDefinitionHandleExceptionIsThrown() {
        when(kpiDefinitionApi.addKpiDefinitionsWithHttpInfo(any(JsonNode.class))).thenThrow(TokenAuthenticationException.class);

        assertThatThrownBy(() -> objectUnderTest.postKpiDefinitions())
                .isInstanceOf(KpiDefinitionHandlingException.class)
                .hasCauseInstanceOf(TokenAuthenticationException.class)
                .hasMessage("Failed to send KPI definitions");

        verify(kpiDefinitionApi, times(3)).addKpiDefinitionsWithHttpInfo(any(JsonNode.class));
    }

    private void assertKpiDefinitionNode(final JsonNode node, final String name, final Tuple... aliasAndKpiCount) {
        final ArrayNode outputTablesArray = node.withArray("kpi_output_tables");

        assertThat(IteratorUtils.toList(outputTablesArray.elements()))
                .as(name)
                .hasSize(aliasAndKpiCount.length)
                .extracting(
                        n -> n.get("alias").asText(),
                        n -> n.withArray("kpi_definitions").size())
                .containsExactly(aliasAndKpiCount);
    }
}