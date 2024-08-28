/*******************************************************************************
 * COPYRIGHT Ericsson 2023 - 2024
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

import static com.ericsson.oss.apps.util.Constants.MetricConstants.APIGATEWAY_SESSIONID_HTTP_REQUESTS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.API_GATEWAY_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.CODE;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.CONFIGURATION_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.CONFIGURATION_ID_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.DESCRIPTION;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.DMM_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.DMM_HTTP_REQUESTS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.FAILED;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.KPI_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.KPI_HTTP_REQUESTS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.METHOD;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.METRIC_DESCRIPTIONS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_HTTP_REQUESTS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_DISCARDED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_KPI_NULL_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_KPI_RETRIEVAL_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_MONITORED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_MONITORED_LOOKBACK_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_PA_EXECUTION_TIME_HOURLY;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_SESSION_CREATED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_SESSION_FINISHED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_SESSION_STARTED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_SESSION_STOPPED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_VERDICT_DEGRADED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_VERDICT_NOT_DEGRADED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_VERDICT_NOT_POSSIBLE_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.REGISTER;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.SESSION_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.SESSION_ID_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.SESSION_ID_STATUS_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.SKIPPED;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.STATUS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.SUCCEEDED;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.TAG;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.TOKENGATEWAY_HTTP_REQUESTS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.TOKEN_GATEWAY_ENDPOINT;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetricService {

    private static final String GET = HttpMethod.GET.toString();
    private static final String POST = HttpMethod.POST.toString();
    private static final String PUT = HttpMethod.PUT.toString();
    private static final String DELETE = HttpMethod.DELETE.toString();
    private static final String HTTP_CODE_200 = "200";
    private static final String HTTP_CODE_201 = "201";
    private static final String HTTP_CODE_202 = "202";
    private static final String HTTP_CODE_204 = "204";
    private static final String HTTP_CODE_400 = "400";
    private static final String HTTP_CODE_404 = "404";
    private static final String HTTP_CODE_409 = "409";
    private static final String HTTP_CODE_415 = "415";
    private static final String HTTP_CODE_500 = "500";
    private static final String HTTP_CODE_503 = "503";
    private final MeterRegistry meterRegistry;
    private final Map<String, Timer.Sample> timedTasks = new ConcurrentHashMap<>();

    @PostConstruct
    @Async
    public void initializeMetrics() {
        log.info("Initializing metrics started");
        registerHttpMetrics(KPI_HTTP_REQUESTS_DURATION_SECONDS, KPI_ENDPOINT, POST,
                HTTP_CODE_201, HTTP_CODE_400, HTTP_CODE_409, HTTP_CODE_500);
        registerHttpMetrics(DMM_HTTP_REQUESTS_DURATION_SECONDS, DMM_ENDPOINT, GET,
                HTTP_CODE_200, HTTP_CODE_400);
        registerHttpMetrics(TOKENGATEWAY_HTTP_REQUESTS_DURATION_SECONDS, TOKEN_GATEWAY_ENDPOINT,
                POST, HTTP_CODE_200, HTTP_CODE_400);
        registerHttpMetrics(APIGATEWAY_SESSIONID_HTTP_REQUESTS_DURATION_SECONDS, API_GATEWAY_ENDPOINT, POST,
                HTTP_CODE_200, HTTP_CODE_500, HTTP_CODE_503);
        registerHttpMetrics(PME_HTTP_REQUESTS_DURATION_SECONDS, CONFIGURATION_ENDPOINT, GET,
                HTTP_CODE_200, HTTP_CODE_500);
        registerHttpMetrics(PME_HTTP_REQUESTS_DURATION_SECONDS, CONFIGURATION_ENDPOINT, POST,
                HTTP_CODE_201, HTTP_CODE_400, HTTP_CODE_415, HTTP_CODE_500);
        registerHttpMetrics(PME_HTTP_REQUESTS_DURATION_SECONDS, CONFIGURATION_ID_ENDPOINT, GET, HTTP_CODE_200, HTTP_CODE_404, HTTP_CODE_500);
        registerHttpMetrics(PME_HTTP_REQUESTS_DURATION_SECONDS, CONFIGURATION_ID_ENDPOINT, PUT, HTTP_CODE_200, HTTP_CODE_400, HTTP_CODE_404,
                HTTP_CODE_409, HTTP_CODE_415, HTTP_CODE_500);
        registerHttpMetrics(PME_HTTP_REQUESTS_DURATION_SECONDS, CONFIGURATION_ID_ENDPOINT, DELETE, HTTP_CODE_204, HTTP_CODE_400, HTTP_CODE_404,
                HTTP_CODE_409, HTTP_CODE_500);
        registerHttpMetrics(PME_HTTP_REQUESTS_DURATION_SECONDS, SESSION_ENDPOINT, GET, HTTP_CODE_200, HTTP_CODE_500, HTTP_CODE_503);
        registerHttpMetrics(PME_HTTP_REQUESTS_DURATION_SECONDS, SESSION_ENDPOINT, POST, HTTP_CODE_201, HTTP_CODE_400, HTTP_CODE_409, HTTP_CODE_415,
                HTTP_CODE_500, HTTP_CODE_503);
        registerHttpMetrics(PME_HTTP_REQUESTS_DURATION_SECONDS, SESSION_ID_ENDPOINT, GET, HTTP_CODE_200, HTTP_CODE_404, HTTP_CODE_500, HTTP_CODE_503);
        registerHttpMetrics(PME_HTTP_REQUESTS_DURATION_SECONDS, SESSION_ID_STATUS_ENDPOINT, PUT, HTTP_CODE_202, HTTP_CODE_400, HTTP_CODE_404,
                HTTP_CODE_409, HTTP_CODE_415, HTTP_CODE_500, HTTP_CODE_503);
        registerMetric(Timer.builder(PME_PA_EXECUTION_TIME_HOURLY), PME_PA_EXECUTION_TIME_HOURLY, STATUS, FAILED);
        registerMetric(Timer.builder(PME_PA_EXECUTION_TIME_HOURLY), PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED);
        registerMetric(Timer.builder(PME_PA_EXECUTION_TIME_HOURLY), PME_PA_EXECUTION_TIME_HOURLY, STATUS, SKIPPED);

        increment(PME_SESSION_CREATED_COUNT, 0);
        increment(PME_SESSION_STARTED_COUNT, 0);
        increment(PME_SESSION_FINISHED_COUNT, 0);
        increment(PME_SESSION_STOPPED_COUNT, 0);
        increment(PME_VERDICT_DEGRADED_COUNT, 0);
        increment(PME_VERDICT_NOT_DEGRADED_COUNT, 0);
        increment(PME_VERDICT_NOT_POSSIBLE_COUNT, 0);
        increment(PME_MO_KPI_RETRIEVAL_COUNT, 0);
        increment(PME_MO_KPI_NULL_COUNT, 0);
        increment(PME_MO_MONITORED_COUNT, 0);
        increment(PME_MO_MONITORED_LOOKBACK_COUNT, 0);
        increment(PME_MO_COUNT, 0);
        increment(PME_MO_DISCARDED_COUNT, 0);

        log.info("Initializing metrics completed");
    }

    public void increment(final String metricName, final String... tags) {
        increment(metricName, 1, tags);
    }

    public void increment(final String metricName, final double value, final String... tags) {
        Optional<Counter> counter = findMetric(Counter.class, metricName, tags);
        if (counter.isEmpty()) {
            final Optional<Object> result = registerMetric(Counter.builder(metricName), metricName, tags);
            counter = result.map(Counter.class::cast);
        }

        counter.ifPresent(counter1 -> counter1.increment(value));
    }

    public void startTimer(final String keyId, final String metricName, final String... tags) {
        if (findTimer(metricName, tags).isEmpty()) {
            registerMetric(Timer.builder(metricName), metricName, tags);
        }
        timedTasks.put(metricName + keyId, Timer.start(meterRegistry));
    }

    public void stopTimer(final String keyId, final String metricName, final String... tags) {
        final Optional<Timer> timer = findTimer(metricName, tags);
        if (timer.isPresent()) {
            timedTasks.get(metricName + keyId).stop(timer.get());
            timedTasks.remove(metricName + keyId);
        } else {
            log.warn("stopTimer(): Timer not found {}, {}, {}", keyId, metricName, tags);
        }
    }

    public Optional<Timer> findTimer(final String metricName, final String... tags) {
        return findMetric(Timer.class, metricName, tags);
    }

    private void registerHttpMetrics(final String timerName, final String endPoint, final String method,
            final String... httpCodes) {
        for (final String httpCode : httpCodes) {
            registerMetric(Timer.builder(timerName), timerName, ENDPOINT, endPoint, METHOD, method, CODE, httpCode);
        }
    }

    public Optional<Counter> findCounter(final String metricName, final String... tags) {
        return findMetric(Counter.class, metricName, tags);
    }

    private <M extends Meter> Optional<M> findMetric(final Class<M> meterClass, final String metricName, final String... tags) {
        final Set<Tag> tagSet = new HashSet<>();
        for (int i = 0; i < tags.length - 1; i += 2) {
            tagSet.add(Tag.of(tags[i], tags[i + 1]));
        }

        final List<M> resultList = meterRegistry.getMeters().stream()
                .filter(meterClass::isInstance)
                .map(meterClass::cast)
                .filter(m -> metricName.equals(m.getId().getName()))
                .filter(m -> tagSet.equals(new HashSet<>(m.getId().getTags()))).toList();

        if (resultList.size() > 1) {
            log.warn("Multiple metrics found for {} {}", metricName, tags);
        }

        return resultList.isEmpty() ? Optional.empty() : Optional.of(resultList.get(0));
    }

    private Optional<Object> registerMetric(final Object builder, final String metricName, final String... tags) {
        Object metricBuilder = builder;
        try {
            for (int i = 0; i < tags.length - 1; i += 2) {
                metricBuilder = metricBuilder.getClass().getMethod(TAG, String.class, String.class)
                        .invoke(metricBuilder, tags[i], tags[i + 1]);
            }

            metricBuilder = metricBuilder.getClass().getMethod(DESCRIPTION, String.class)
                    .invoke(metricBuilder, METRIC_DESCRIPTIONS.get(metricName));
            return Optional.of(builder.getClass().getMethod(REGISTER, MeterRegistry.class).invoke(metricBuilder, meterRegistry));
        } catch (final Exception e) {
            log.error("Metric method invoke error", e);
        }
        return Optional.empty();
    }
}
