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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.Objects;

import com.ericsson.oss.apps.exception.CertificateHandlingException;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class CertificateManagerUtils {
    public static CertificateFactory getCertificateFactory() {
        try {
            return CertificateFactory.getInstance("X.509");
        } catch (final CertificateException e) {
            throw new CertificateHandlingException("Failed to get CertificateFactory instance", e);
        }
    }

    public static Collection<Certificate> loadCertsFromFile(final Path filePath) {
        if (!Files.exists(filePath)) {
            throw new CertificateHandlingException(String.format("File %s does not exist", filePath));
        }

        final String certs;
        try {
            certs = Files.readString(filePath);
        } catch (final IOException e) {
            throw new CertificateHandlingException(String.format("Failed to read file %s", filePath), e);
        }

        try {
            final byte[] certsBytes = certs.getBytes(StandardCharsets.UTF_8);
            final var bytes = new ByteArrayInputStream(certsBytes);
            final CertificateFactory certificateFactory = CertificateManagerUtils.getCertificateFactory();
            return (Collection<Certificate>) certificateFactory.generateCertificates(bytes);
        } catch (final CertificateException e) {
            throw new CertificateHandlingException("Failed to create Certificates from file", e);
        }
    }

    public static KeyStore loadStore(final String storePath, final String storePass, final String keyStoreType) {
        final KeyStore store;
        try {
            store = KeyStore.getInstance(keyStoreType);
        } catch (final KeyStoreException e) {
            throw new CertificateHandlingException("Failed to get Keystore instance", e);
        }
        try (final var inputStream = Objects.isNull(storePath) ? null : Files.newInputStream(Paths.get(storePath))) {
            store.load(inputStream, storePass.toCharArray());
        } catch (final IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new CertificateHandlingException("Failed to load Keystore", e);
        }
        return store;
    }
}
