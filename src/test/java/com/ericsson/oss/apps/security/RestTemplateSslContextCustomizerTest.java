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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import lombok.val;

/**
 * Unit tests for {@link RestTemplateSslContextCustomizer} class.
 */
@ExtendWith(MockitoExtension.class)
class RestTemplateSslContextCustomizerTest {
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private TlsConfig tlsConfig;

    @InjectMocks
    private RestTemplateSslContextCustomizer objectUnderTest;

    @Test
    void whenUpdateRestTemplates_verifyTemplatesUpdatesWithSameRequestFactory() throws KeyStoreException {
        val keystore = mock(KeyStore.class);
        val templateOne = mock(RestTemplate.class);
        val templateTwo = mock(RestTemplate.class);

        when(keystore.aliases()).thenReturn(Collections.emptyEnumeration());

        when(applicationContext.getBeansOfType(RestTemplate.class))
                .thenReturn(Map.of("templateOne", templateOne, "templateTwo", templateTwo));
        when(tlsConfig.getClientProtocol()).thenReturn("TLSv1.3");

        objectUnderTest.updateRestTemplates(keystore);

        val argCaptor = ArgumentCaptor.forClass(ClientHttpRequestFactory.class);

        verify(applicationContext, times(1)).getBeansOfType(RestTemplate.class);
        verify(templateOne, times(1)).setRequestFactory(argCaptor.capture());
        verify(templateTwo, times(1)).setRequestFactory(argCaptor.capture());

        assertThat(argCaptor.getAllValues().stream().distinct().count()).isOne();
    }
}
