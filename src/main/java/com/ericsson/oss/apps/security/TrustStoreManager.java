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

package com.ericsson.oss.apps.security;

import static com.ericsson.oss.apps.util.Constants.JKS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.ericsson.oss.apps.exception.CertificateHandlingException;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(value = "tls.enabled", havingValue = "true")
@RequiredArgsConstructor
public class TrustStoreManager {
    private final TrustStoreConfig trustStoreConfig;
    private final RestTemplateSslContextCustomizer restTemplateSslContextCustomizer;
    private final Set<Certificate> addedCerts = new HashSet<>();

    @PostConstruct
    void instantiateNewStore() {
        final var trustStore = CertificateManagerUtils.loadStore(null, trustStoreConfig.getAppStorePass(), JKS);
        saveTrustStore(trustStore);
    }

    void loadCertsFromFile(final Path platformCertFilePath) {
        final Collection<Certificate> certificates = CertificateManagerUtils.loadCertsFromFile(platformCertFilePath);
        addCertificates(certificates);
    }

    private void saveTrustStore(final KeyStore trustStore) {
        try (final var outputStream = Files.newOutputStream(Paths.get(trustStoreConfig.getAppStorePath()))) {
            trustStore.store(outputStream, trustStoreConfig.getAppStorePass().toCharArray());
        } catch (final IOException | NoSuchAlgorithmException | KeyStoreException | CertificateException e) {
            log.error("Failed to save trust store::", e);
            throw new CertificateHandlingException("Failed to save keystore file", e);
        }
        restTemplateSslContextCustomizer.updateRestTemplates(trustStore);
    }

    private void addCertificates(final Collection<Certificate> certs) {
        final var trustStore = CertificateManagerUtils.loadStore(trustStoreConfig.getAppStorePath(), trustStoreConfig.getAppStorePass(),
                JKS);
        final Set<Certificate> successfullyAddedCerts = new HashSet<>();
        boolean shouldUpdate = false;

        for (final Certificate cert : certs) {
            final boolean newCert = !addedCerts.contains(cert);
            if (newCert) {
                try {
                    trustStore.setCertificateEntry(String.valueOf(UUID.randomUUID()), cert);
                    shouldUpdate = true;
                    successfullyAddedCerts.add(cert);
                } catch (final KeyStoreException e) {
                    log.error("Failed to set certificate entry in truststore", e);
                }
            } else {
                log.warn("Duplicate certificate was accepted but not added");
            }
        }

        if (shouldUpdate) {
            saveTrustStore(trustStore);
            addedCerts.addAll(successfullyAddedCerts);
            log.info("Certificates are added and accepted successfully");
        }
    }
}