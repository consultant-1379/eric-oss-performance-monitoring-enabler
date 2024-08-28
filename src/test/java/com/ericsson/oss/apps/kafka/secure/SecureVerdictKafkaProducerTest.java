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

package com.ericsson.oss.apps.kafka.secure;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.ericsson.oss.apps.model.VerdictMessage;

@ExtendWith(MockitoExtension.class)
class SecureVerdictKafkaProducerTest {
    @Mock
    private KafkaTemplate<String, VerdictMessage> kafkaTemplate;

    private static final String VERDICT_TOPIC = "epme-verdicts";

    private SecureVerdictKafkaProducer objectUnderTest;

    @BeforeEach
    void setUp() {
        objectUnderTest = new SecureVerdictKafkaProducer(VERDICT_TOPIC, kafkaTemplate);
    }

    @Test
    void whenSendVerdict_verifyKafkaTemplateSendIsCalled() {
        final var message = mock(VerdictMessage.class);

        objectUnderTest.sendVerdict(message);

        verify(kafkaTemplate, times(1)).send(VERDICT_TOPIC, message);
        verifyNoMoreInteractions(kafkaTemplate);
        verifyNoInteractions(message);
    }
}