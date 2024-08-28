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

package com.ericsson.oss.apps.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class KeyStoreManagerTestUtils {

    public static void cleanUpTempKeystore(final String store) throws IOException {
        final Path tempKeyStoreName = Paths.get(store);
        if (Files.exists(tempKeyStoreName)) {
            Files.delete(tempKeyStoreName);
        }
    }

    public static void assertCertCountInTempKeystore(final String tempStore, final String tempPass, final int expected, final String keyStoreType)
            throws KeyStoreException {
        assertThat(getNumberOfCertsInKeystore(tempStore, tempPass, keyStoreType))
                .isEqualTo(expected);

    }

    public static void assertKeyCountInTempKeyStore(final String tempStore, final String tempPass, final int expected, final String keyStoreType)
            throws KeyStoreException {
        assertThat(getNumberOfKeysInKeystore(tempStore, tempPass, keyStoreType))
                .isEqualTo(expected);
    }

    private static Integer getNumberOfCertsInKeystore(final String path, final String password, final String keyStoreType) throws KeyStoreException {
        final KeyStore keyStore;
        try {
            keyStore = loadKeyStore(path, password, keyStoreType);
        } catch (final CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new AssertionError("Should not have thrown exception", e);
        }

        final AtomicInteger certCount = new AtomicInteger(0);
        keyStore.aliases().asIterator().forEachRemaining(cert -> certCount.incrementAndGet());

        return certCount.get();
    }

    private static Integer getNumberOfKeysInKeystore(final String path, final String password, final String keyStoreType) throws KeyStoreException {
        final KeyStore keyStore;
        try {
            keyStore = loadKeyStore(path, password, keyStoreType);
        } catch (final CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new AssertionError("Should not have thrown exception", e);
        }

        final Enumeration<String> aliases = keyStore.aliases();
        final AtomicInteger keyCount = new AtomicInteger(0);

        while (aliases.hasMoreElements()) {
            final String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                keyCount.incrementAndGet();
            }
        }

        return keyCount.get();
    }

    public static List<X509Certificate> getCertsInKeystore(final String path, final String password, final String keyStoreType)
            throws KeyStoreException {
        final KeyStore keyStore;
        try {
            keyStore = loadKeyStore(path, password, keyStoreType);
        } catch (final CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new AssertionError("Should not have thrown exception", e);
        }
        final List<X509Certificate> inStore = new ArrayList<>();
        keyStore.aliases().asIterator().forEachRemaining(alias -> {
            try {
                inStore.add((X509Certificate) keyStore.getCertificate(alias));
            } catch (final KeyStoreException e) {
                throw new AssertionError(String.format("Should not have thrown exception for alias (%s)", alias), e);
            }
        });

        return inStore;
    }

    public static Key getKeyInKeyStore(final String path, final String password, final String keyStoreType) throws KeyStoreException {
        final KeyStore keyStore;
        try {
            keyStore = loadKeyStore(path, password, keyStoreType);
        } catch (final CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new AssertionError("Should not have thrown exception", e);
        }
        final Key[] key = new Key[1];
        keyStore.aliases().asIterator().forEachRemaining(alias -> {
            try {
                key[0] = keyStore.getKey(alias, password.toCharArray());
            } catch (final KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
                throw new AssertionError(String.format("Should not have thrown exception for alias (%s)", alias), e);
            }
        });
        return key[0];
    }

    public static List<X509Certificate> loadCertificate(final String path, final String subPaths) throws CertificateException {
        final Path certPath = Paths.get(path, subPaths);
        final String certs;
        try {
            certs = Files.readString(certPath);
        } catch (final IOException e) {
            throw new AssertionError("Should not have thrown exception", e);
        }

        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        return certificateFactory.generateCertificates(new ByteArrayInputStream(certs.getBytes(StandardCharsets.UTF_8)))
                .stream()
                .filter(c -> c instanceof X509Certificate)
                .map(c -> (X509Certificate) c)
                .collect(Collectors.toList());
    }

    public static Key loadKey(final String path, final String subPath) throws InvalidKeySpecException, NoSuchAlgorithmException {
        final Path keyPath = Paths.get(path, subPath);
        final String key;

        try {
            key = Files.readString(keyPath);
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            final String keyTrimmed = key.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\n", "");
            final byte[] decodedBytes = Base64.getMimeDecoder().decode(keyTrimmed.getBytes());
            final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedBytes);
            return keyFactory.generatePrivate(keySpec);
        } catch (final IOException e) {
            throw new AssertionError("Should not have thrown exception", e);
        }
    }

    private static KeyStore loadKeyStore(final String path, final String password, final String keyStoreType)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        final KeyStore store = KeyStore.getInstance(keyStoreType);
        try (final var inputStream = Files.newInputStream(Paths.get(path))) {
            store.load(inputStream, password.toCharArray());
        }
        return store;
    }

}
