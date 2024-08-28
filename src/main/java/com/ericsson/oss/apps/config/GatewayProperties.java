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

import static com.ericsson.oss.apps.util.Constants.COLON;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Configuration
@ConfigurationProperties(prefix = "gateway")
@Slf4j
public class GatewayProperties {
    private String scheme;
    private String host;
    private String port;
    private Map<String, Service> services = new HashMap<>();

    public String getUrl() {
        final StringBuilder builder = new StringBuilder();
        if (scheme != null && !scheme.isEmpty()) {
            builder.append(scheme).append("://");
        }
        builder.append(host);
        if (port != null && !port.isEmpty()) {
            builder.append(COLON).append(port);
        }
        return builder.toString();
    }

    public String getBasePath(final String serviceName) {
        final Service service = getService(serviceName);
        return (isNullOrBlank(service.getUrl()) ? getUrl() : service.getUrl()) + service.getBasePath();
    }

    public String removeServicePath(final String serviceName) {
        final Service service = getService(serviceName);
        return (isNullOrBlank(service.getUrl()) ? getUrl() : service.getUrl());
    }

    public Service getService(final String name) {
        return services.getOrDefault(name, null);
    }

    private static boolean isNullOrBlank(final String string) {
        return string == null || string.isBlank();
    }

    @Data
    public static class Service {
        private String url;
        private String basePath;
        private String headers;

        public Map<String, String> getHeadersAsMap() {
            try {
                final ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(isNullOrBlank(headers) ? "{}" : headers, Map.class);
            } catch (final JsonProcessingException e) {
                log.error("Failed to parse headers", e);
                return new HashMap<>();
            }
        }
    }
}
