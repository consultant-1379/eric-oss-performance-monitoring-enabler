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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

import com.ericsson.oss.apps.exception.MessageBusHandlingException;
import com.ericsson.oss.apps.model.VerdictMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.event.ConsumerStartedEvent;
import org.springframework.kafka.event.ConsumerStoppedEvent;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.ericsson.oss.apps.exception.KafkaStartupHandlingException;
import com.ericsson.oss.apps.kafka.secure.MonitoringObjectKafkaConsumer;
import com.ericsson.oss.apps.kafka.secure.SecureVerdictKafkaProducer;

@ExtendWith(MockitoExtension.class)
public class KafkaStartupServiceTest {

    private static final String MESSAGE_BUS_HOST = "bootstrap.stsvp1eic28.stsoss.sero.gic.ericsson.se:443";

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Mock
    private MonitoringObjectKafkaConsumer monitoringObjectKafkaConsumer;

    @Mock
    private MessageBusService messageBusService;

    @Mock
    private KafkaTemplate<String, VerdictMessage> kafkaTemplate;

    @Mock
    private ProducerFactory<String, VerdictMessage> producerFactory;

    private KafkaStartupService objectUnderTest;

    @BeforeEach
    void setUp() {
        objectUnderTest = new KafkaStartupService(applicationContext, threadPoolTaskScheduler, messageBusService,true);
        KafkaStartupService.CONSUMER_BEAN_STARTED.set(false);
        KafkaStartupService.LAST_CONSUMER_CHECK_TIME.set(null);
    }

    @Test
    void whenStartSecureKafka_andTlsEnabledAndConsumerAndProducerAreStarted_thenSuccess() throws IllegalAccessException {
        assertThat(KafkaStartupService.CONSUMER_BEAN_STARTED.get()).isFalse();
        when(applicationContext.getBean(MonitoringObjectKafkaConsumer.class)).thenReturn(monitoringObjectKafkaConsumer);
        KafkaStartupService.RETRY_CONSUMER_CONNECTION.set(false);

        setValueField(objectUnderTest, "messageBusService", messageBusService);
        when(messageBusService.getMessageBusEndpoint()).thenReturn(MESSAGE_BUS_HOST);
        when(applicationContext.getBean("secureVerdictKafkaTemplate", KafkaTemplate.class)).thenReturn(kafkaTemplate);
        when(kafkaTemplate.getProducerFactory()).thenReturn(producerFactory);

        objectUnderTest.startSecureKafka();

        assertThat(KafkaStartupService.CONSUMER_BEAN_STARTED.get()).isTrue();
        verify(applicationContext, times(1)).getBean(MonitoringObjectKafkaConsumer.class);
        verify(kafkaTemplate, times(1)).getProducerFactory();
        verify(producerFactory, times(1)).updateConfigs(any(Map.class));
    }

    @Test
    void whenStartSecureKafka_andTlsEnabledAndConsumerBeanNotCreated_thenExceptionIsThrown() {
        assertThat(KafkaStartupService.CONSUMER_BEAN_STARTED.get()).isFalse();
        doThrow(new BeanCreationException("Unable to create bean"))
                .when(applicationContext).getBean(MonitoringObjectKafkaConsumer.class);

        assertThatThrownBy(() -> objectUnderTest.startSecureKafka())
                .isInstanceOf(KafkaStartupHandlingException.class)
                .hasMessage("Failed to start Kafka in secure mode");
        assertThat(KafkaStartupService.CONSUMER_BEAN_STARTED.get()).isFalse();
    }

    @Test
    void whenStartSecureKafka_andTlsEnabledAndProducerBeanNotCreated_thenExceptionIsThrown() throws IllegalAccessException {
        KafkaStartupService.CONSUMER_BEAN_STARTED.set(true);
        KafkaStartupService.RETRY_CONSUMER_CONNECTION.set(false);

        setValueField(objectUnderTest, "messageBusService", messageBusService);
        when(messageBusService.getMessageBusEndpoint()).thenReturn(MESSAGE_BUS_HOST);

        doThrow(new BeanCreationException("Unable to create bean"))
                .when(applicationContext).getBean("secureVerdictKafkaTemplate", KafkaTemplate.class);

        assertThatThrownBy(() -> objectUnderTest.startSecureKafka())
                .isInstanceOf(KafkaStartupHandlingException.class)
                .hasMessage("Failed to start Kafka in secure mode");
    }

    @Test
    void whenStartSecureKafka_andTlsEnabledAndMessageBusServiceCallFails_thenExceptionIsThrown() throws IllegalAccessException {
        KafkaStartupService.CONSUMER_BEAN_STARTED.set(true);
        KafkaStartupService.RETRY_CONSUMER_CONNECTION.set(false);

        setValueField(objectUnderTest, "messageBusService", messageBusService);

        doThrow(new MessageBusHandlingException("Failed to get message bus"))
                .when(messageBusService).getMessageBusEndpoint();

        assertThatThrownBy(() -> objectUnderTest.startSecureKafka())
                .isInstanceOf(KafkaStartupHandlingException.class)
                .hasMessage("Failed to start Kafka in secure mode");
    }

    @Test
    void whenStartSecureKafka_andTlsEnabledAndConsumerIsNotStarted_thenExceptionIsThrown() {
        KafkaStartupService.CONSUMER_BEAN_STARTED.set(true);
        KafkaStartupService.RETRY_CONSUMER_CONNECTION.set(true);

        assertThatThrownBy(() -> objectUnderTest.startSecureKafka())
                .isInstanceOf(KafkaStartupHandlingException.class)
                .hasMessage("The Kafka consumer is not ready");
    }

    @Test
    void whenStartSecureKafka_andTlsDisabled_thenNothingHappens() {
        objectUnderTest = new KafkaStartupService(applicationContext, threadPoolTaskScheduler, messageBusService,false);
        objectUnderTest.startSecureKafka();

        verify(applicationContext, never()).getBean(MonitoringObjectKafkaConsumer.class);
        verify(applicationContext, never()).getBean(SecureVerdictKafkaProducer.class);
    }

    @Test
    void whenConsumerStarted_thenVerifyItIsScheduledForCheck() throws IllegalAccessException {
        assertThat(KafkaStartupService.LAST_CONSUMER_CHECK_TIME.get()).isNull();
        final ConsumerStartedEvent consumerStartedEvent = mock(ConsumerStartedEvent.class);
        setValueField(objectUnderTest, "expectedStartupTimeInSeconds", 3L);

        objectUnderTest.consumerStarted(consumerStartedEvent);

        assertThat(KafkaStartupService.LAST_CONSUMER_CHECK_TIME.get()).isNotNull();

        verify(threadPoolTaskScheduler, times(1)).schedule(any(), any(Instant.class));
    }

    @Test
    void whenConsumerStopped_thenVerifyItIsScheduledForCheck() throws IllegalAccessException {
        assertThat(KafkaStartupService.LAST_CONSUMER_CHECK_TIME.get()).isNull();
        final ConsumerStoppedEvent consumerStoppedEvent = mock(ConsumerStoppedEvent.class);
        setValueField(objectUnderTest, "retryWaitPeriodInSeconds", 2L);

        objectUnderTest.consumerStopped(consumerStoppedEvent);

        verify(threadPoolTaskScheduler, times(1)).schedule(any(), any(Instant.class));

        assertThat(KafkaStartupService.LAST_CONSUMER_CHECK_TIME.get()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void whenConsumerRestart_thenProcessIsManagedAndRestarted(final boolean hasChildProcess) {
        final ConsumerStoppedEvent consumerStoppedEvent = mock(ConsumerStoppedEvent.class);
        final KafkaMessageListenerContainer<?, ?> kafkaMessageListenerContainer = mock(KafkaMessageListenerContainer.class);

        when(consumerStoppedEvent.getSource(KafkaMessageListenerContainer.class)).thenReturn(kafkaMessageListenerContainer);
        when(kafkaMessageListenerContainer.isChildRunning()).thenReturn(hasChildProcess);

        objectUnderTest.consumerRestart(consumerStoppedEvent).run();

        final VerificationMode verificationMode = hasChildProcess ? times(1) : never();
        verify(kafkaMessageListenerContainer, verificationMode).stop();
        verify(kafkaMessageListenerContainer, times(1)).start();
    }

    @ParameterizedTest
    @MethodSource("com.ericsson.oss.apps.service.KafkaStartupServiceTest#timePeriods")
    public void whenConsumerStartedCheck_thenDetermineIfFurtherCheckIsRequired(final Map.Entry<Instant, Boolean> timePeriods) throws IllegalAccessException {
        KafkaStartupService.LAST_CONSUMER_CHECK_TIME.set(timePeriods.getKey());
        setValueField(objectUnderTest, "retryWaitPeriodInSeconds", 2L);
        setValueField(objectUnderTest, "expectedStartupTimeInSeconds", 3L);

        objectUnderTest.consumerStartedCheck();

        assertThat(KafkaStartupService.RETRY_CONSUMER_CONNECTION.get()).isEqualTo(timePeriods.getValue());

        final VerificationMode verificationMode = timePeriods.getValue() ? times(1) : never();
        verify(threadPoolTaskScheduler, verificationMode).schedule(any(), any(Instant.class));
    }

    static Stream<Map.Entry<Instant, Boolean>> timePeriods() {
        return Stream.of(
                Map.entry(Instant.now().minusSeconds(100), false),
                Map.entry(Instant.now().plusSeconds(100), true));
    }

    private void setValueField(final KafkaStartupService kafkaStartupService, final String name, final Object value) throws IllegalAccessException {
        final Field field = ReflectionUtils
                .findFields(KafkaStartupService.class, f -> f.getName().equals(name),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);

        field.setAccessible(true);
        field.set(kafkaStartupService, value);
    }
}