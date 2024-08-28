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

package com.ericsson.oss.apps.security;

import static com.ericsson.oss.apps.util.Constants.CERT_FILE_CHECK_SCHEDULE_IN_SECONDS;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ericsson.oss.apps.exception.CertificateHandlingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(value = "tls.enabled", havingValue = "true")
@RequiredArgsConstructor
public class TrustStoreCertificateFileChangeDetector {
    private final TrustStoreManager trustManager;
    private final TrustStoreConfig trustStoreConfig;
    private final Map<Path, FileTime> lastAcceptedModifiedTime = new HashMap<>();

    private final KafkaTlsConfig kafkaTlsConfig;

    @Value("${kafkaStartup.tls.enabled}")
    private boolean kafkaTlsEnabled;

    @Scheduled(fixedRate = CERT_FILE_CHECK_SCHEDULE_IN_SECONDS, initialDelayString = "${startup.initialDelayInSeconds}", timeUnit = TimeUnit.SECONDS)
    public void checkForUpdates() {
        log.info("Checking for certificate updates");
        final List<Path> certificatePaths = new ArrayList<>();
        certificatePaths.add(Paths.get(trustStoreConfig.getCertFilePath()));

        if (kafkaTlsEnabled) {
            certificatePaths.add(Paths.get(kafkaTlsConfig.getKafkaCertFilePath()));
        }

        certificatePaths.forEach(certPath -> {
            try (final DirectoryStream<Path> dirStream = Files.newDirectoryStream(certPath, "*.crt")) {
                for (final Path path : dirStream) {
                    if (!Files.isDirectory(path)) {
                        final FileTime fileLastModifiedTime = Files.getLastModifiedTime(path);
                        if (fileIsChanged(path, fileLastModifiedTime)) {
                            loadCerts(path, fileLastModifiedTime);
                        }
                    }
                }
                log.info("Completed check for certificate updates in directory {}", certPath);
            } catch (final IOException e) {
                log.error("Failed to read directory {}. Unable to perform certificate updates.", certPath, e);
            }
        });
    }

    void loadCerts(final Path path, final FileTime fileLastModifiedTime) {
        try {
            trustManager.loadCertsFromFile(path);
            setLastModifiedTime(path, fileLastModifiedTime);
        } catch (final CertificateHandlingException e) {
            log.error("Failed to reload and update modified certificates for {}", path, e);
        }
    }

    private boolean fileIsChanged(final Path certFilePath, final FileTime fileLastModifiedTime) {
        if (lastAcceptedModifiedTime.containsKey(certFilePath) && fileLastModifiedTime.equals(lastAcceptedModifiedTime.get(certFilePath))) {
            log.info("certificate file {} last modified timestamp has not changed, certificate will not be reloaded", certFilePath);
            return false;
        }

        log.info("Certificate file {} last modified timestamp has changed, reloading certificate", certFilePath);
        return true;
    }

    //created for unit test
    void setLastModifiedTime(final Path certFilePath, final FileTime fileLastModifiedTime) {
        lastAcceptedModifiedTime.put(certFilePath, fileLastModifiedTime);
    }

    void setKafkaTlsEnabled(final boolean kafkaTlsEnabled) {
        this.kafkaTlsEnabled = kafkaTlsEnabled;
    }
}