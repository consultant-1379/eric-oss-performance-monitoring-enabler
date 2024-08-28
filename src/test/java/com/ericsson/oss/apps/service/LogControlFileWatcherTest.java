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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for {@link LogControlFileWatcher} class.
 */
@ActiveProfiles("test")
class LogControlFileWatcherTest {

    public static final String CURRENT_LOG_LEVEL = "currentLogLevel";
    private static final String INFO = "INFO";

    private LogControlFileWatcher objectUnderTest;

    @BeforeEach
    public void setUp() {
        objectUnderTest = new LogControlFileWatcher();
    }

    @Test
    void whenValidLogControlFileChange_thenLogLevelShouldChange() throws IOException {
        final String debug = "DEBUG";
        final String error = "ERROR";

        updateLogControlFile(debug);

        assertThat(objectUnderTest).hasFieldOrPropertyWithValue(CURRENT_LOG_LEVEL, INFO);

        objectUnderTest.reloadLogControlFile();

        assertThat(objectUnderTest).hasFieldOrPropertyWithValue(CURRENT_LOG_LEVEL, debug);

        objectUnderTest.reloadLogControlFile();

        assertThat(objectUnderTest).hasFieldOrPropertyWithValue(CURRENT_LOG_LEVEL, debug);

        updateLogControlFile(error);

        objectUnderTest.reloadLogControlFile();

        assertThat(objectUnderTest).hasFieldOrPropertyWithValue(CURRENT_LOG_LEVEL, error);
    }

    @Test
    void whenInvalidLogControlFileChange_thenLogLevelShouldNotChange() throws IOException {
        updateLogControlFile("INVALID_SEVERITY");

        assertThat(objectUnderTest).hasFieldOrPropertyWithValue(CURRENT_LOG_LEVEL, INFO);

        objectUnderTest.reloadLogControlFile();

        assertThat(objectUnderTest).hasFieldOrPropertyWithValue(CURRENT_LOG_LEVEL, INFO);
    }

    private void updateLogControlFile(final String severity) throws IOException {
        final File tempFile = File.createTempFile("logcontrol", ".tmp");

        final String logControlFileContent = "[{\"container\": \"eric-oss-performance-monitoring-enabler\",\"severity\": \"" + severity + "\"}]";
        Files.write(tempFile.toPath(), logControlFileContent.getBytes(StandardCharsets.UTF_8));
        objectUnderTest.setLogControlFileName(tempFile.getAbsolutePath());
    }
}
