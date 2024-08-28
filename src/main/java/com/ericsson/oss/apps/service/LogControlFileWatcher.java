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

import static com.ericsson.oss.apps.util.Constants.FACILITY_KEY;
import static com.ericsson.oss.apps.util.Constants.LOG_CONTROL_FILE_CHECK_SCHEDULE_IN_SECONDS;
import static com.ericsson.oss.apps.util.Constants.NON_AUDIT_LOG;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LogControlFileWatcher {

    @Value("${logging.logcontrol-file}")
    private String logControlFileName = "";
    private String currentLogLevel = "INFO";

    @Scheduled(fixedRate = LOG_CONTROL_FILE_CHECK_SCHEDULE_IN_SECONDS, timeUnit = TimeUnit.SECONDS)
    public void reloadLogControlFile() {
        final ObjectMapper mapper = new ObjectMapper();
        final Path logControlFilePath = Path.of(logControlFileName);
        if (logControlFilePath.toFile().exists()) {
            try {
                final LogControl[] logControls = mapper.readValue(logControlFilePath.toFile(), LogControl[].class);
                for (final LogControl logControl : logControls) {
                    updateLogLevel(logControl.getSeverity());
                }
            } catch (final IOException e) {
                log.error("Unable to read logControl file: " + logControlFileName, e);
            }
        }
    }

    private void updateLogLevel(final String severity) {
        if (currentLogLevel.equals(severity)) {
            MDC.put(FACILITY_KEY, NON_AUDIT_LOG);
            log.debug("The log level is the same as before ({}), no change needed", currentLogLevel);
            MDC.remove(FACILITY_KEY);
            return;
        }

        final SupportedLogLevel logLevel;
        try {
            logLevel = SupportedLogLevel.valueOf(severity.toUpperCase(Locale.US));
        } catch (final IllegalArgumentException e) {
            MDC.put(FACILITY_KEY, NON_AUDIT_LOG);
            log.error("Not supported log level: {}", severity);
            MDC.remove(FACILITY_KEY);
            return;
        }
        final Level level = Level.toLevel(logLevel.name());
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (final Logger logger : loggerContext.getLoggerList()) {
            logger.setLevel(level);
        }
        currentLogLevel = logLevel.toString();
        MDC.put(FACILITY_KEY, NON_AUDIT_LOG);
        log.warn("The log level has been changed to {}", level.toString());
        MDC.remove(FACILITY_KEY);

    }

    public void setLogControlFileName(final String logControlFileName) {
        if (validateFileName(logControlFileName)) {
            this.logControlFileName = logControlFileName;
        }
    }

    private boolean validateFileName(final String logControlFileName) {
        return (logControlFileName != null && !logControlFileName.isBlank());
    }

    @Data
    static class LogControl {
        private String container;
        private String severity;
    }
}