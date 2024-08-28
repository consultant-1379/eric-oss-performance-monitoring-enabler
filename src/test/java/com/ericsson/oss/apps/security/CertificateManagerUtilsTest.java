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

import java.security.KeyStore;

import org.junit.jupiter.api.Test;

import com.ericsson.oss.apps.exception.CertificateHandlingException;

/**
 * Unit tests for {@link CertificateManagerUtilsTest} class.
 */
class CertificateManagerUtilsTest {

    private static final String VALID_KEYSTORE_PATH = "src/test/resources/tls/truststore/cacerts";
    private static final String INVALID_KEYSTORE_PATH = "src/test/resources/tls/truststore";
    private static final String PASSWORD = "changeit";

    @Test
    void whenValidTruststoreIsLoaded_thenTrustStoreIsReturned() {
        final KeyStore truststore = CertificateManagerUtils.loadStore(VALID_KEYSTORE_PATH, PASSWORD, JKS);
        assertThat(truststore).isNotNull();
    }

    @Test
    void whenKeyStoreIsLoaded_andKeystoreTypeIsInvalid_verifyCertificateHandlingExceptionIsThrown() {
        assertThatThrownBy(() -> CertificateManagerUtils.loadStore(VALID_KEYSTORE_PATH, PASSWORD, "invalid_keystore_type"))
                .isInstanceOf(CertificateHandlingException.class)
                .hasMessage("Failed to get Keystore instance");
    }

    @Test
    void whenKeyStoreIsLoaded_andKeystoreDoesNotExist_verifyCertificateHandlingExceptionIsThrown() {
        assertThatThrownBy(() -> CertificateManagerUtils.loadStore(INVALID_KEYSTORE_PATH, PASSWORD, JKS))
                .isInstanceOf(CertificateHandlingException.class)
                .hasMessage("Failed to load Keystore");
    }

    @Test
    void whenKeyStoreIsLoaded_andPasswordIsIncorrect_verifyCertificateHandlingExceptionIsThrown() {
        assertThatThrownBy(() -> CertificateManagerUtils.loadStore(VALID_KEYSTORE_PATH, "pass", JKS))
                .isInstanceOf(CertificateHandlingException.class)
                .hasMessage("Failed to load Keystore");
    }

}
