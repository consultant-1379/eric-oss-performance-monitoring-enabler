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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.apps.exception.CertificateHandlingException;
import com.ericsson.oss.apps.util.TrustManagerTestUtils;

/**
 * Unit tests for {@link TrustStoreManager} class.
 */

@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext
@SpringBootTest(properties = {
        "tls.enabled=true",
        "tls.truststore.appStorePath=" + TrustStoreManagerTest.APP_TRUST_STORE,
        "tls.truststore.appStorePass=testPassword"
})
class TrustStoreManagerTest {
    static final String APP_TRUST_STORE_FILE_NAME = "truststore.jks";
    private static final String RESOURCE_FILE_PATH = "src/test/resources/tls/";
    static final String CERT_RESOURCE_FILE_PATH = RESOURCE_FILE_PATH + "truststore_certs/";
    private static final String TRUSTSTORE_RESOURCE_FILE_PATH = RESOURCE_FILE_PATH + "truststore/";
    static final String APP_TRUST_STORE = TRUSTSTORE_RESOURCE_FILE_PATH + APP_TRUST_STORE_FILE_NAME;
    private static final String CERT_TO_UPDATE_RESOURCE_FILE_PATH = RESOURCE_FILE_PATH + "truststore_certs-to-update/";
    private static final String BUNDLE_CERT_FILE_NAME = "bundle.crt";
    private static final String CERT_TO_UPDATE_FILE_NAME = "google.crt";
    private static final String INVALID_CERT_FILE_NAME = "not-a-certificate.crt";
    private static final String NON_EXISTENT_RESOURCE_FILE_PATH = "src/test/resources/tls/not-a-valid-directory/";

    private static final int INITIAL_CERT_COUNT = 0;
    private static final int UPDATED_CERT_COUNT = 10;
    private static final int FINAL_CERT_COUNT = 11;

    @Value("${tls.truststore.appStorePass}")
    private String appTrustStorePass;

    @SpyBean
    private TrustStoreManager objectUnderTest;

    @SpyBean
    private TlsConfig tlsConfig;
    @SpyBean
    private RestTemplateSslContextCustomizer restTemplateSslContextCustomizer;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeAll
    static void beforeAll() throws IOException {
        TrustManagerTestUtils.cleanUpTempKeystore(APP_TRUST_STORE);
    }

    @AfterAll
    static void afterAll() throws IOException {
        TrustManagerTestUtils.cleanUpTempKeystore(APP_TRUST_STORE);
    }

    @BeforeEach
    void setUp() {
        reset(tlsConfig, objectUnderTest, restTemplateSslContextCustomizer);
    }

    @Test
    @Order(0)
    void whenTrustStoreIsLoaded_verifyTrustStoreWrittenToFile() throws KeyStoreException {
        objectUnderTest.instantiateNewStore();
        verify(restTemplateSslContextCustomizer, times(1))
                .updateRestTemplates(any(KeyStore.class));
        TrustManagerTestUtils.assertCertCountInTempKeystore(APP_TRUST_STORE, appTrustStorePass, INITIAL_CERT_COUNT, JKS);
    }

    @Test
    @Order(1)
    void whenTrustStoreIsLoadedAndPopulatedWithCerts_verifyTrustStoreWrittenToFile() throws KeyStoreException, IOException {
        TrustManagerTestUtils.assertCertCountInTempKeystore(APP_TRUST_STORE, appTrustStorePass, INITIAL_CERT_COUNT, JKS);

        objectUnderTest.loadCertsFromFile(Path.of(CERT_RESOURCE_FILE_PATH, BUNDLE_CERT_FILE_NAME));
        verify(restTemplateSslContextCustomizer, times(1))
                .updateRestTemplates(any(KeyStore.class));

        TrustManagerTestUtils.assertCertCountInTempKeystore(APP_TRUST_STORE, appTrustStorePass, UPDATED_CERT_COUNT, JKS);
    }

    @Test
    @Order(2)
    void whenTrustStoreIsLoaded_verifyTrustManagerContainsBundleCerts() throws CertificateException, KeyStoreException {
        final List<X509Certificate> bundleCertificates = TrustManagerTestUtils.loadCertificate(CERT_RESOURCE_FILE_PATH, BUNDLE_CERT_FILE_NAME);
        final List<X509Certificate> inTrustStore = TrustManagerTestUtils.getCertsInKeystore(APP_TRUST_STORE, appTrustStorePass,
                JKS);
        assertThat(inTrustStore).containsAll(bundleCertificates);
    }

    @Test
    @Order(3)
    void whenTrustStoreIsLoaded_verifyTrustManagerDoesNotContainUpdatedCert() throws CertificateException, KeyStoreException {
        final List<X509Certificate> toUpdateCerts = TrustManagerTestUtils.loadCertificate(CERT_TO_UPDATE_RESOURCE_FILE_PATH,
                CERT_TO_UPDATE_FILE_NAME);
        final List<X509Certificate> inTrustStore = TrustManagerTestUtils.getCertsInKeystore(APP_TRUST_STORE, appTrustStorePass,
                JKS);
        assertThat(inTrustStore).isNotEmpty().doesNotContainAnyElementsOf(toUpdateCerts);
    }

    @Test
    @Order(4)
    void whenTrustStoreLoadCertsIsCalled_andAlreadyInitialized_verifyKeystoreIsUpdated()
            throws NoSuchAlgorithmException, KeyStoreException, IOException {
        TrustManagerTestUtils.assertCertCountInTempKeystore(APP_TRUST_STORE, appTrustStorePass, UPDATED_CERT_COUNT, JKS);

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        final Map<String, ClientHttpRequestFactory> templateRequestFactories = applicationContext.getBeansOfType(RestTemplate.class)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getRequestFactory()));

        objectUnderTest
                .loadCertsFromFile(Path.of(CERT_TO_UPDATE_RESOURCE_FILE_PATH, CERT_TO_UPDATE_FILE_NAME));

        verify(restTemplateSslContextCustomizer, times(1))
                .updateRestTemplates(any(KeyStore.class));

        // Verify SSL context is updated
        final SSLContext updatedSslContext = SSLContext.getInstance("TLS");
        assertThat(updatedSslContext).isNotEqualTo(sslContext);

        // Verify ClientHttpRequestFactory is updated in RestTemplates
        final Map<String, RestTemplate> restTemplates = applicationContext.getBeansOfType(RestTemplate.class);
        assertThat(restTemplates)
                .hasSize(templateRequestFactories.size())
                .containsOnlyKeys(templateRequestFactories.keySet());

        final SoftAssertions softly = new SoftAssertions();

        for (final String beanKey : templateRequestFactories.keySet()) {
            softly.assertThat(restTemplates)
                    .as("missing entry satisfying for key '%s'", beanKey)
                    .hasEntrySatisfying(beanKey,
                            template -> assertThat(template.getRequestFactory())
                                    .isNotEqualTo(templateRequestFactories.get(beanKey)));
        }

        softly.assertAll();

        // Verify new certificate has been added
        TrustManagerTestUtils.assertCertCountInTempKeystore(APP_TRUST_STORE, appTrustStorePass, FINAL_CERT_COUNT, JKS);
    }

    @Test
    @Order(5)
    void whenTrustStoreLoadCertsIsCalled_andCertificateIsDuplicated_verifyKeystoreIsNotUpdated() throws KeyStoreException, IOException {
        TrustManagerTestUtils.assertCertCountInTempKeystore(APP_TRUST_STORE, appTrustStorePass, FINAL_CERT_COUNT, JKS);

        objectUnderTest.loadCertsFromFile(Paths.get(CERT_TO_UPDATE_RESOURCE_FILE_PATH + CERT_TO_UPDATE_FILE_NAME));

        verify(restTemplateSslContextCustomizer, never()).updateRestTemplates(any(KeyStore.class));

        // Verify certificate count is the same
        TrustManagerTestUtils.assertCertCountInTempKeystore(APP_TRUST_STORE, appTrustStorePass, FINAL_CERT_COUNT, JKS);
    }

    @Test
    @Order(6)
    void whenTrustStoreLoadCertsIsCalled_andCertificateIsNotValid_verifyKeystoreIsNotUpdated() throws KeyStoreException, IOException {
        TrustManagerTestUtils.assertCertCountInTempKeystore(APP_TRUST_STORE, appTrustStorePass, FINAL_CERT_COUNT, JKS);

        assertThatThrownBy(() -> {
            objectUnderTest.loadCertsFromFile(Paths.get(CERT_TO_UPDATE_RESOURCE_FILE_PATH + INVALID_CERT_FILE_NAME));
        }).isInstanceOf(CertificateHandlingException.class).hasMessage("Failed to create Certificates from file")
                .hasCauseInstanceOf(CertificateException.class);

        verify(restTemplateSslContextCustomizer, never()).updateRestTemplates(any(KeyStore.class));

        // Verify certificate count is the same
        TrustManagerTestUtils.assertCertCountInTempKeystore(APP_TRUST_STORE, appTrustStorePass, FINAL_CERT_COUNT, JKS);
    }

    @Test
    @Order(7)
    void whenTrustStoreLoadCertsIsCalled_andDirectoryDoesNotExist_verifyKeystoreIsNotUpdated() throws KeyStoreException {
        TrustManagerTestUtils.assertCertCountInTempKeystore(APP_TRUST_STORE, appTrustStorePass, FINAL_CERT_COUNT, JKS);

        assertThatThrownBy(() -> objectUnderTest.loadCertsFromFile(Paths.get(NON_EXISTENT_RESOURCE_FILE_PATH)))
                .isInstanceOf(CertificateHandlingException.class)
                // using regex matching here as filepath printing handles differently per OS
                .hasMessageMatching("File .+not-a-valid-directory does not exist");

        verify(restTemplateSslContextCustomizer, never()).updateRestTemplates(any(KeyStore.class));

        // Verify certificate count is the same
        TrustManagerTestUtils.assertCertCountInTempKeystore(APP_TRUST_STORE, appTrustStorePass, FINAL_CERT_COUNT, JKS);
    }
}
