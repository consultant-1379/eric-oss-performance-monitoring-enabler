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

package com.ericsson.oss.apps.util;

import static com.ericsson.oss.apps.util.Constants.CONNECTION_TIME_TO_LIVE_SECONDS;
import static com.ericsson.oss.apps.util.Constants.CONNECT_TIMEOUT_SECONDS;
import static com.ericsson.oss.apps.util.Constants.MAX_TOTAL_CONNECTIONS;
import static com.ericsson.oss.apps.util.Constants.MAX_TOTAL_CONNECTIONS_PER_ROUTE;
import static com.ericsson.oss.apps.util.Constants.REQUEST_TIMEOUT_SECONDS;
import static com.ericsson.oss.apps.util.Constants.SOCKET_TIMEOUT_SECONDS;
import static com.ericsson.oss.apps.util.Constants.STANDARD_COOKIE_SPEC;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultClientConnectionReuseStrategy;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import lombok.experimental.UtilityClass;

@UtilityClass
public class HttpClientUtils {
    private static final ConnectionConfig CONNECTION_CONFIG = ConnectionConfig.custom()
            .setTimeToLive(TimeValue.ofSeconds(CONNECTION_TIME_TO_LIVE_SECONDS))
            .setConnectTimeout(Timeout.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .setSocketTimeout(Timeout.ofSeconds(SOCKET_TIMEOUT_SECONDS))
            .build();

    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setCookieSpec(STANDARD_COOKIE_SPEC)
            .setConnectionRequestTimeout(Timeout.ofSeconds(REQUEST_TIMEOUT_SECONDS))
            .build();

    public static HttpClientConnectionManager createConnectionManager(final SSLConnectionSocketFactory sslSocketFactory) {
        return PoolingHttpClientConnectionManagerBuilder
                .create()
                .setSSLSocketFactory(sslSocketFactory)
                .setMaxConnTotal(MAX_TOTAL_CONNECTIONS)
                .setMaxConnPerRoute(MAX_TOTAL_CONNECTIONS_PER_ROUTE)
                .setConnectionConfigResolver(httpRoute -> CONNECTION_CONFIG)
                .build();
    }

    public static HttpClient createClient(final HttpClientConnectionManager connectionManager) {
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(REQUEST_CONFIG)
                .setConnectionReuseStrategy(DefaultClientConnectionReuseStrategy.INSTANCE)
                .build();
    }
}