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

package com.ericsson.oss.apps.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.ericsson.oss.apps.exception.CertificateHandlingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(value = "tls.enabled", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class KeyStoreManager {

    private final KeyStoreConfig keyStoreConfig;

    Collection<Certificate> loadCertsFromFile(final Path platformCertFilePath) {
        return CertificateManagerUtils.loadCertsFromFile(platformCertFilePath);
    }

    Key loadKeyFromFile(final Path filePath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        final String key = Files.readString(filePath);
        final String keyTrimmed = key.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replace("\n", "")
                .replace("\r", "");
        final byte[] decodedBytes = Base64.getDecoder().decode(keyTrimmed);

        final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedBytes);
        return keyFactory.generatePrivate(keySpec);
    }

    void saveKeyStore(final KeyStore keyStore) {
        try (final var outputStream = Files.newOutputStream(Paths.get(keyStoreConfig.getAppStorePath()))) {
            keyStore.store(outputStream, keyStoreConfig.getAppKeyStorePass().toCharArray());
        } catch (final IOException | NoSuchAlgorithmException | KeyStoreException | CertificateException e) {
            throw new CertificateHandlingException("Failed to save keystore file", e);
        }
    }

    void updateKeyStore(final Collection<Certificate> certs, final Key key) {
        final var keyStore = CertificateManagerUtils.loadStore(keyStoreConfig.getAppStorePath(), keyStoreConfig.getAppKeyStorePass(),
                KeyStore.getDefaultType());
        final Certificate[] certArray = certs.toArray(new Certificate[0]);

        try {
            keyStore.setKeyEntry("rapp", key, keyStoreConfig.getAppKeyStorePass().toCharArray(), certArray);
            saveKeyStore(keyStore);
            log.info("Certificate and key added and accepted successfully");
        } catch (final KeyStoreException e) {
            log.error("Failed to set certificate and key entry in keystore", e);
        }
    }
}
