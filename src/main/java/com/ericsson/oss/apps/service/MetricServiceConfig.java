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
package com.ericsson.oss.apps.service;

import static com.ericsson.oss.apps.util.Constants.MetricConstants.ACTUATOR_PREFIX;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.APIGATEWAY_SESSIONID_HTTP_REQUESTS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.API_GATEWAY_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.CODE;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.DMM_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.DMM_HTTP_REQUESTS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.HTTP_CLIENT_REQUEST;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.HTTP_SERVER_REQUEST;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.KPI_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.KPI_HTTP_REQUESTS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.METHOD;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PERFORMANCE_MONITOR_PREFIX;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.STATUS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.TOKEN_GATEWAY_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.TOKEN_GATEWAY_REQUESTS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.URI;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class MetricServiceConfig {

    @Bean
    public MeterFilter meterFilter() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(final Meter.Id id) {
                final String metricName = id.getName();
                if (id.getName().equals(HTTP_SERVER_REQUEST) || id.getName().equals(HTTP_CLIENT_REQUEST)) {
                    return processMetric(id, metricName);
                }
                return id;
            }
        };
    }

    private Meter.Id processMetric(final Meter.Id id, final String name) {
        String metricName = name;
        final List<Tag> newTags = new ArrayList<>();
        String prefixName = PERFORMANCE_MONITOR_PREFIX;
        for (final Tag tag : id.getTags()) {
            switch (tag.getKey()) {
                case URI:
                    String baseEndPoint = tag.getValue().split("\\?")[0];
                    if (tag.getValue().startsWith("/actuator")) {
                        prefixName = ACTUATOR_PREFIX;
                    } else if (tag.getValue().startsWith(KPI_ENDPOINT)) {
                        metricName = KPI_HTTP_REQUESTS;
                        baseEndPoint = tag.getValue();
                    } else if (tag.getValue().startsWith(TOKEN_GATEWAY_ENDPOINT)) {
                        metricName = TOKEN_GATEWAY_REQUESTS;
                        baseEndPoint = TOKEN_GATEWAY_ENDPOINT;
                    } else if (tag.getValue().startsWith(DMM_ENDPOINT)) {
                        metricName = DMM_HTTP_REQUESTS;
                        baseEndPoint = DMM_ENDPOINT;
                    } else if (tag.getValue().startsWith(API_GATEWAY_ENDPOINT)) {
                        metricName = APIGATEWAY_SESSIONID_HTTP_REQUESTS;
                        baseEndPoint = API_GATEWAY_ENDPOINT;
                    }
                    newTags.add(Tag.of(ENDPOINT, baseEndPoint));
                    break;
                case STATUS:
                    newTags.add(Tag.of(CODE, tag.getValue()));
                    break;
                case METHOD:
                    newTags.add(tag);
                    break;
                default:
            }
        }
        if (id.getName().equals(HTTP_SERVER_REQUEST)) {
            metricName = "http.requests";
        }
        return id.replaceTags(newTags).withName(prefixName + metricName + ".duration");
    }
}
