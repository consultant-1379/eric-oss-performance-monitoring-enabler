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

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.HttpsSupport;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.apps.exception.CertificateHandlingException;
import com.ericsson.oss.apps.util.HttpClientUtils;

import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(value = "tls.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RestTemplateSslContextCustomizer {
    private final ApplicationContext context;
    private final TlsConfig tlsConfig;

    void updateRestTemplates(final KeyStore trustStore) {
        final SSLContext sslContext = createSslContext(trustStore);
        final HttpClientConnectionManager connectionManager = HttpClientUtils.createConnectionManager(createSslConnectionSocketFactory(sslContext));
        final HttpClient client = HttpClientUtils.createClient(connectionManager);
        final ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(client);

        for (final RestTemplate restTemplate : context.getBeansOfType(RestTemplate.class).values()) {
            restTemplate.setRequestFactory(requestFactory);
        }
    }

    private SSLContext createSslContext(final KeyStore trustStore) {
        try {
            return SSLContexts.custom()
                    .setProtocol(tlsConfig.getClientProtocol())
                    .setSecureRandom(new SecureRandom())
                    .loadTrustMaterial(trustStore, null)
                    .build();
        } catch (final NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new CertificateHandlingException("Failed to create new SSLContext with truststore", e);
        }
    }

    private SSLConnectionSocketFactory createSslConnectionSocketFactory(final SSLContext sslContext) {
        return new SSLConnectionSocketFactory(sslContext, new String[] { tlsConfig.getClientProtocol() }, null,
                HttpsSupport.getDefaultHostnameVerifier());
    }
}
