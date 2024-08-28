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
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.HttpsSupport;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContexts;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.apps.util.HttpClientUtils;

@Configuration
@ConditionalOnProperty(value = "tls.enabled", havingValue = "false")
public class InsecureRestTemplateCustomizer implements RestTemplateCustomizer {
    private final ClientHttpRequestFactory clientHttpRequestFactory;

    public InsecureRestTemplateCustomizer() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        final SSLContext sslContext = SSLContexts.custom()
                .setSecureRandom(new SecureRandom())
                .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                .build();

        final HttpClientConnectionManager connectionManager = HttpClientUtils
                .createConnectionManager(new SSLConnectionSocketFactory(sslContext, HttpsSupport.getDefaultHostnameVerifier()));
        final HttpClient httpClient = HttpClientUtils.createClient(connectionManager);

        clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    @Override
    public void customize(final RestTemplate restTemplate) {
        restTemplate.setRequestFactory(clientHttpRequestFactory);
    }
}
