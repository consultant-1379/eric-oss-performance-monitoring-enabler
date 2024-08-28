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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.oss.apps.exception.CertificateHandlingException;

/**
 * Unit tests for {@link TrustStoreCertificateFileChangeDetector} class.
 */
@ExtendWith(MockitoExtension.class)
class KeyStoreCertificateFileChangeDetectorTest {
    private static final String CERTS_PATH = "src/test/resources/tls/keystore_cert_and_key";
    private static final String DIR_CERTS_PATH = "src/test/resources/tls";
    private static final String NON_EXISTENT_DIR_PATH = "src/test/resources/tls/not-a-valid-directory";
    private static final String CERT_NO_KEY_PATH = "src/test/resources/tls/keystore_cert_no_key";
    private static final String KEY_NO_CERT_PATH = "src/test/resources/tls/keystore_key_no_cert";
    private static final String KEYSTORE_PATH = "src/test/resources/tls/keystore/keystore.p12";
    private static final String PASSWORD = "password";

    @Mock
    private KeyStoreManager customX509KeyManager;

    @Mock
    private KeyStoreConfig keyStoreConfig;

    @Spy
    @InjectMocks
    private KeyStoreCertificateFileChangeDetector objectUnderTest;

    @BeforeEach
    public void setUp() {
        reset(customX509KeyManager, objectUnderTest);
    }

    @Test
    void whenFileTimeStampHasChanged_verifyLoadCertsFromFileIsCalled() throws IOException {
        when(keyStoreConfig.getCertFilePath()).thenReturn(CERTS_PATH);
        when(keyStoreConfig.getAppStorePath()).thenReturn("src/test/resources/keystore");
        setFileModifiedTime();
        objectUnderTest.checkForUpdates();
        verify(customX509KeyManager, times(1)).loadCertsFromFile(any(Path.class));
    }

    @Test
    void whenFileTimeStampNotChanged_verifyLoadCertsFromFileNotCalled() {
        when(keyStoreConfig.getCertFilePath()).thenReturn(CERTS_PATH);
        when(keyStoreConfig.getAppKeyStorePass()).thenReturn("pass");
        objectUnderTest.checkForUpdates();
        reset(customX509KeyManager, objectUnderTest);
        objectUnderTest.checkForUpdates();
        verifyNoMoreInteractions(customX509KeyManager);
    }

    @Test
    void whenCheckForUpdates_andTrustManagerThrowsCertificateHandlingException_verifyLastModifiedTimeIsNotCalled() throws IOException {
        when(keyStoreConfig.getCertFilePath()).thenReturn(CERTS_PATH);
        when(keyStoreConfig.getAppKeyStorePass()).thenReturn("pass");
        doThrow(CertificateHandlingException.class).when(customX509KeyManager).loadCertsFromFile(any(Path.class));
        setFileModifiedTime();

        objectUnderTest.checkForUpdates();
        verify(objectUnderTest, never()).setLastModifiedTime(any(Path.class), any(FileTime.class));
    }

    @Test
    void whenCheckForUpdates_andTrustManagerThrowsNoSuchAlgorithmException_verifyUpdateKeyStoreIsNotCalled()
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        when(keyStoreConfig.getCertFilePath()).thenReturn(CERTS_PATH);
        when(keyStoreConfig.getAppKeyStorePass()).thenReturn("pass");
        doThrow(NoSuchAlgorithmException.class).when(customX509KeyManager).loadKeyFromFile(any(Path.class));
        setFileModifiedTime();

        objectUnderTest.checkForUpdates();

        verify(customX509KeyManager, never()).updateKeyStore(any(Collection.class), any(Key.class));
    }

    @Test
    void whenCheckForUpdates_andCertFilePathContainsOnlyDirectories_verifyLastModifiedTimeAndLoadCertsIsNotCalled() throws IOException {
        when(keyStoreConfig.getCertFilePath()).thenReturn(DIR_CERTS_PATH);
        setFileModifiedTime();

        objectUnderTest.checkForUpdates();

        verify(customX509KeyManager, never()).updateKeyStore(any(Collection.class), any(Key.class));
        verify(objectUnderTest, never()).setLastModifiedTime(any(Path.class), any(FileTime.class));
    }

    @Test
    void whenCheckForUpdates_andCertFilePathDoesNotExist_verifyLastModifiedTimeAndLoadCertsIsNotCalled() throws IOException {
        when(keyStoreConfig.getCertFilePath()).thenReturn(NON_EXISTENT_DIR_PATH);
        setFileModifiedTime();

        objectUnderTest.checkForUpdates();

        verify(customX509KeyManager, never()).loadCertsFromFile(any(Path.class));
        verify(objectUnderTest, never()).setLastModifiedTime(any(Path.class), any(FileTime.class));
    }

    @Test
    void whenCheckForUpdates_andCertDoesNotExist_verifyLastModifiedTimeAndLoadCertsIsNotCalled()
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        when(keyStoreConfig.getCertFilePath()).thenReturn(CERT_NO_KEY_PATH);
        objectUnderTest.checkForUpdates();
        verify(customX509KeyManager, never()).loadKeyFromFile(any(Path.class));
        verify(customX509KeyManager, never()).loadCertsFromFile(any(Path.class));
        verify(objectUnderTest, never()).setLastModifiedTime(any(Path.class), any(FileTime.class));
    }

    @Test
    void whenCheckForUpdates_andKeyDoesNotExist_verifyLastModifiedTimeAndLoadCertsIsNotCalled()
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        when(keyStoreConfig.getCertFilePath()).thenReturn(KEY_NO_CERT_PATH);
        objectUnderTest.checkForUpdates();
        verify(customX509KeyManager, never()).loadKeyFromFile(any(Path.class));
        verify(customX509KeyManager, never()).loadCertsFromFile(any(Path.class));
        verify(objectUnderTest, never()).setLastModifiedTime(any(Path.class), any(FileTime.class));
    }

    @Test
    void whenKeyStoreIsNotNull_andLastModifiedTimeIsNotSet_verifyLastModifiedTimeIsCalled_andLoadCertsIsNotCalled()
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        when(keyStoreConfig.getCertFilePath()).thenReturn(CERTS_PATH);
        when(keyStoreConfig.getAppStorePath()).thenReturn(KEYSTORE_PATH);
        when(keyStoreConfig.getAppKeyStorePass()).thenReturn(PASSWORD);
        objectUnderTest.checkForUpdates();
        verify(objectUnderTest, times(1)).setLastModifiedTime(any(Path.class), any(FileTime.class));
        verify(customX509KeyManager, never()).loadKeyFromFile(any(Path.class));
        verify(customX509KeyManager, never()).loadCertsFromFile(any(Path.class));

    }

    private void setFileModifiedTime() throws IOException {
        try (final DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(CERTS_PATH))) {
            for (final Path path : dirStream) {
                final FileTime currentFileLastModifiedTime = Files.getLastModifiedTime(path);
                final Instant timeBeforeCurrentFileLastModifiedTime = currentFileLastModifiedTime.toInstant().minus(Duration.ofMinutes(120));
                final FileTime newFileTime = FileTime.fromMillis(timeBeforeCurrentFileLastModifiedTime.toEpochMilli());
                objectUnderTest.setLastModifiedTime(path, newFileTime);
            }
        }
        reset(objectUnderTest);
    }
}
