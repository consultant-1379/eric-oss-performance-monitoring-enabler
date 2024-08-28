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

import static com.ericsson.oss.apps.util.Constants.CERT_FILE_CHECK_SCHEDULE_IN_SECONDS;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ericsson.oss.apps.exception.CertificateHandlingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "tls.enabled", havingValue = "true")
@ConditionalOnExpression("'${logging.streamingMethod}'=='direct' || '${logging.streamingMethod}'=='dual'")
public class KeyStoreCertificateFileChangeDetector {
    private final KeyStoreManager keyManager;
    private final KeyStoreConfig keyStoreConfig;
    private final Map<Path, FileTime> lastAcceptedModifiedTime = new HashMap<>();

    @Scheduled(fixedRate = CERT_FILE_CHECK_SCHEDULE_IN_SECONDS, initialDelayString = "${startup.initialDelayInSeconds}", timeUnit = TimeUnit.SECONDS)
    public void checkForUpdates() {
        log.info("Checking for certificate updates");
        final Path dirPath = Paths.get(keyStoreConfig.getCertFilePath());
        Path certPath = null;
        Path keyPath = null;

        try (final DirectoryStream<Path> dirStream = Files.newDirectoryStream(dirPath)) {

            for (final Path path : dirStream) {
                if (!Files.isDirectory(path)) {
                    if (path.toString().endsWith("crt")) {
                        certPath = path;
                    } else if (path.toString().endsWith("key")) {
                        keyPath = path;
                    }
                }
            }

            if (isKeyAndCertValid(certPath, keyPath) && fileIsChanged(certPath, Files.getLastModifiedTime(certPath))) {
                final Collection<Certificate> cert = loadCert(certPath, Files.getLastModifiedTime(certPath));
                final Key key = loadKey(keyPath, Files.getLastModifiedTime(keyPath));
                keyManager.updateKeyStore(cert, key);
            }

            log.info("Completed check for keystore certificate and key updates");
        } catch (final CertificateHandlingException e) {
            log.error("Failed to load certificate {}. Unable to perform certificate and key updates.", certPath, e);
        } catch (final NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("Failed to load key {}. Unable to perform certificate and key updates.", keyPath, e);
        } catch (final IOException e) {
            log.error("Failed to read directory {}. Unable to perform certificate and key updates.", dirPath, e);
        }
    }

    private Collection<Certificate> loadCert(final Path path, final FileTime fileLastModifiedTime)
            throws CertificateHandlingException {
        final Collection<Certificate> certificates = keyManager.loadCertsFromFile(path);
        setLastModifiedTime(path, fileLastModifiedTime);
        return certificates;
    }

    private Key loadKey(final Path path, final FileTime fileLastModifiedTime) throws InvalidKeySpecException, IOException, NoSuchAlgorithmException {
        final Key key = keyManager.loadKeyFromFile(path);
        setLastModifiedTime(path, fileLastModifiedTime);
        return key;
    }

    private boolean fileIsChanged(final Path filePath, final FileTime fileLastModifiedTime) {
        if (lastAcceptedModifiedTime.containsKey(filePath) && fileLastModifiedTime.equals(lastAcceptedModifiedTime.get(filePath))) {
            log.info("certificate file {} last modified timestamp has not changed, certificate and key will not be reloaded", filePath);
            return false;
        }

        final KeyStore keystore = readKeyStore();

        //Since we are creating the keystore at deploy time we need to make sure we don't duplicate the cert and key on first load
        if (!Objects.isNull(keystore) && lastAcceptedModifiedTime.isEmpty()) {
            log.info("certificate file {} last modified timestamp has not changed, certificate and key will not be reloaded", filePath);
            setLastModifiedTime(filePath, fileLastModifiedTime);
            return false;
        }

        log.info("Certificate file {} last modified timestamp has changed, reloading certificate and key", filePath);
        return true;
    }

    private KeyStore readKeyStore() {
        KeyStore keystore = null;

        try {
            keystore = CertificateManagerUtils.loadStore(keyStoreConfig.getAppStorePath(), keyStoreConfig.getAppKeyStorePass(),
                    KeyStore.getDefaultType());
        } catch (final CertificateHandlingException e) {
            log.warn("Failed to read keystore", e);
        }
        return keystore;
    }

    private boolean isKeyAndCertValid(final Path certPath, final Path keyPath) {
        if (Objects.isNull(certPath)) {
            log.error("Could not find cert. Unable to perform certificate and key updates.");
            return false;
        }

        if (Objects.isNull(keyPath)) {
            log.error("Could not find key. Unable to perform certificate and key updates.");
            return false;
        }
        return true;
    }

    //created for unit test
    void setLastModifiedTime(final Path filePath, final FileTime fileLastModifiedTime) {
        lastAcceptedModifiedTime.put(filePath, fileLastModifiedTime);
    }
}