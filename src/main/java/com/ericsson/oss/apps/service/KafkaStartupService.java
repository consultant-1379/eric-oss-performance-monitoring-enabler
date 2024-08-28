/*******************************************************************************
 * COPYRIGHT Ericsson 2024
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

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.event.ConsumerStartedEvent;
import org.springframework.kafka.event.ConsumerStoppedEvent;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.ericsson.oss.apps.exception.KafkaStartupHandlingException;
import com.ericsson.oss.apps.exception.MessageBusHandlingException;
import com.ericsson.oss.apps.kafka.secure.MonitoringObjectKafkaConsumer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class KafkaStartupService {

    static final AtomicBoolean CONSUMER_BEAN_STARTED = new AtomicBoolean();
    static final AtomicBoolean RETRY_CONSUMER_CONNECTION = new AtomicBoolean(true);

    static final AtomicReference<Instant> LAST_CONSUMER_CHECK_TIME = new AtomicReference<>();

    private final ApplicationContext applicationContext;

    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;

    private final MessageBusService messageBusService;

    private final Boolean tlsEnabled;

    @Value("${kafkaStartup.retry.waitPeriodInSeconds}")
    private Long retryWaitPeriodInSeconds;
    @Value("${kafkaStartup.retry.expectedStartupTimeInSeconds}")
    private Long expectedStartupTimeInSeconds;

    public KafkaStartupService(final ApplicationContext applicationContext,
                               final ThreadPoolTaskScheduler threadPoolTaskScheduler,
                               final MessageBusService messageBusService,
                               @Value("${kafkaStartup.tls.enabled}") final Boolean tlsEnabled) {
        this.applicationContext = applicationContext;
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
        this.messageBusService = messageBusService;
        this.tlsEnabled = tlsEnabled;
    }

    @Retryable(retryFor = {KafkaStartupHandlingException.class},
            maxAttemptsExpression = "${kafkaStartup.retry.maxAttempts}",
            backoff = @Backoff(delay = 10_000, maxDelay = 600_000, multiplier = 2))
    public void startSecureKafka() {
        if (tlsEnabled) {
            log.info("Attempting to start secure Kafka");
            if (!CONSUMER_BEAN_STARTED.get()) {
                startConsumerBean();
            }

            if (RETRY_CONSUMER_CONNECTION.get()) {
                throw new KafkaStartupHandlingException("The Kafka consumer is not ready");
            } else {
                log.info("PME secure Kafka consumer is ready");
            }

            updateProducerBean();
            log.info("PME secure Kafka producer is ready");
        }
    }

    @EventListener
    public void consumerStarted(final ConsumerStartedEvent consumerStartedEvent) {
        final Instant startTime = Instant.now();
        LAST_CONSUMER_CHECK_TIME.set(startTime);

        final Runnable consumerStartedCheck = this::consumerStartedCheck;
        // Set the check 10 seconds into the future in case the consumer goes down with an UNAUTHORIZED error
        threadPoolTaskScheduler.schedule(consumerStartedCheck, startTime.plusSeconds(expectedStartupTimeInSeconds));
    }

    @EventListener
    public void consumerStopped(final ConsumerStoppedEvent consumerStoppedEvent) {
        log.info("PME Kafka Consumer has stopped");

        final Instant startTime = Instant.now().plusSeconds(retryWaitPeriodInSeconds);
        log.info("PME Kafka Consumer will retry again in '{}' seconds", retryWaitPeriodInSeconds);
        LAST_CONSUMER_CHECK_TIME.set(startTime);

        threadPoolTaskScheduler.schedule(consumerRestart(consumerStoppedEvent), startTime);
    }

    private void startConsumerBean() throws KafkaStartupHandlingException {
        try {
            log.info("Starting PME Kafka consumer");
            applicationContext.getBean(MonitoringObjectKafkaConsumer.class);
            CONSUMER_BEAN_STARTED.set(true);
        } catch (final BeansException e) {
            log.warn("Failed to start secure PME Kafka consumer", e);
            throw new KafkaStartupHandlingException("Failed to start Kafka in secure mode", e);
        }
    }

    private void updateProducerBean() throws KafkaStartupHandlingException {
        try {
            log.info("Starting PME Kafka producer");

            final String messageBusEndpoint = messageBusService.getMessageBusEndpoint();
            final KafkaTemplate<?, ?> kafkaTemplate = applicationContext.getBean("secureVerdictKafkaTemplate", KafkaTemplate.class);

            kafkaTemplate.getProducerFactory()
                    .updateConfigs(Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, messageBusEndpoint));
        } catch (final MessageBusHandlingException | BeansException e) {
            log.warn("Failed to start secure PME Kafka producer", e);
            throw new KafkaStartupHandlingException("Failed to start Kafka in secure mode", e);
        }
    }

    void consumerStartedCheck() {
        final Instant now = Instant.now();
        final boolean retryCheck = LAST_CONSUMER_CHECK_TIME.get().isAfter(now);
        RETRY_CONSUMER_CONNECTION.set(retryCheck);

        if (retryCheck) {
            final Runnable consumerStartedCheck = this::consumerStartedCheck;
            final long initialBackOff = retryWaitPeriodInSeconds + expectedStartupTimeInSeconds;
            threadPoolTaskScheduler.schedule(consumerStartedCheck, now.plusSeconds(initialBackOff));
        }
    }

    Runnable consumerRestart(final ConsumerStoppedEvent consumerStoppedEvent) {
        return () -> {
            final KafkaMessageListenerContainer<?, ?> kafkaMessageListenerContainer =
                    consumerStoppedEvent.getSource(KafkaMessageListenerContainer.class);

            if (kafkaMessageListenerContainer.isChildRunning()) {
                kafkaMessageListenerContainer.stop();
            }
            kafkaMessageListenerContainer.start();
        };
    }

}
