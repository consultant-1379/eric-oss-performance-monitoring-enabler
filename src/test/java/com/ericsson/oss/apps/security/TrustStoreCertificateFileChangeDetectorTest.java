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
import java.time.Duration;
import java.time.Instant;

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
class TrustStoreCertificateFileChangeDetectorTest {
    private static final String CERT_PATH = "src/test/resources/tls/truststore_certs-to-update";
    private static final String DIR_CERTS_PATH = "src/test/resources/tls";
    private static final String NON_EXISTENT_DIR_PATH = "src/test/resources/tls/not-a-valid-directory";

    @Mock
    private TrustStoreManager trustStoreManager;

    @Mock
    private TrustStoreConfig trustStoreConfig;

    @Mock
    private KafkaTlsConfig kafkaTlsConfig;

    @Spy
    @InjectMocks
    private TrustStoreCertificateFileChangeDetector objectUnderTest;

    @BeforeEach
    void setup() {
        when(trustStoreConfig.getCertFilePath()).thenReturn(CERT_PATH);
        when(kafkaTlsConfig.getKafkaCertFilePath()).thenReturn(CERT_PATH);
        objectUnderTest.checkForUpdates();
        reset(trustStoreManager, objectUnderTest);
        objectUnderTest.setKafkaTlsEnabled(true);
    }

    @Test
    void whenFileTimeStampHasChanged_verifyLoadCertsFromFileIsCalled() throws IOException {
        setFileModifiedTime();
        objectUnderTest.checkForUpdates();
        verify(trustStoreManager, times(2)).loadCertsFromFile(any(Path.class));
    }

    @Test
    void whenFileTimeStampNotChanged_verifyLoadCertsFromFileNotCalled() {
        objectUnderTest.checkForUpdates();
        verifyNoMoreInteractions(trustStoreManager);
    }

    @Test
    void whenCheckForUpdates_andTrustManagerThrowsCertificateHandlingException_verifyLastModifiedTimeIsNotCalled() throws IOException {
        doThrow(CertificateHandlingException.class)
                .when(trustStoreManager).loadCertsFromFile(any(Path.class));
        setFileModifiedTime();
        objectUnderTest.checkForUpdates();
        verify(objectUnderTest, never()).setLastModifiedTime(any(Path.class), any(FileTime.class));
    }

    @Test
    void whenCheckForUpdates_andCertFilePathContainsOnlyDirectories_verifyLastModifiedTimeAndLoadCertsIsNotCalled() throws IOException {
        when(trustStoreConfig.getCertFilePath()).thenReturn(DIR_CERTS_PATH);
        when(kafkaTlsConfig.getKafkaCertFilePath()).thenReturn(DIR_CERTS_PATH);
        setFileModifiedTime();

        objectUnderTest.checkForUpdates();

        verify(trustStoreManager, never()).loadCertsFromFile(any(Path.class));
        verify(objectUnderTest, never()).setLastModifiedTime(any(Path.class), any(FileTime.class));
    }

    @Test
    void whenCheckForUpdates_andCertFilePathDoesNotExist_verifyLastModifiedTimeAndLoadCertsIsNotCalled() throws IOException {
        when(trustStoreConfig.getCertFilePath()).thenReturn(NON_EXISTENT_DIR_PATH);
        when(kafkaTlsConfig.getKafkaCertFilePath()).thenReturn(NON_EXISTENT_DIR_PATH);
        setFileModifiedTime();

        objectUnderTest.checkForUpdates();

        verify(trustStoreManager, never()).loadCertsFromFile(any(Path.class));
        verify(objectUnderTest, never()).setLastModifiedTime(any(Path.class), any(FileTime.class));
    }

    private void setFileModifiedTime() throws IOException {
        try (final DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(CERT_PATH))) {
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
