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

package com.ericsson.oss.apps.config;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.apps.config.client.TokenAuthenticationInterceptor;
import com.ericsson.oss.apps.security.InsecureRestTemplateCustomizer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RestTemplateConfiguration {

    @Bean
    @ConditionalOnProperty(value = "tls.enabled", havingValue = "false")
    public InsecureRestTemplateCustomizer requestFactoryCustomizer()
            throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return new InsecureRestTemplateCustomizer();
    }

    @Bean(name = "restTemplateConfig")
    public RestTemplate restTemplate(final RestTemplateBuilder builder,
            final ObjectProvider<TokenAuthenticationInterceptor> authenticationInterceptors) {
        final RestTemplate restTemplate = builder.build();
        final List<ClientHttpRequestInterceptor> existingInterceptors = restTemplate.getInterceptors();
        final List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(existingInterceptors);
        final Optional<TokenAuthenticationInterceptor> authenticationInterceptor = authenticationInterceptors.orderedStream().findAny();
        if (authenticationInterceptor.isPresent()) {
            final TokenAuthenticationInterceptor interceptor = authenticationInterceptor.get();
            if (!existingInterceptors.contains(interceptor)) {
                interceptors.add(interceptor);
            }
        }
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }

    @Bean(name = "restTemplateNoAuthInterceptor")
    public RestTemplate restTemplateNoAuthInterceptor(final RestTemplateBuilder builder) {
        return builder.build();
    }
}
