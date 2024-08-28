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

import com.ericsson.oss.apps.model.MonitoringObjectMessage;
import com.ericsson.oss.apps.service.MonitoringObjectKafkaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MonitoringObjectKafkaConsumerTest {

    @Mock
    private MonitoringObjectKafkaService monitoringObjectKafkaService;

    @InjectMocks
    private MonitoringObjectKafkaConsumer objectUnderTest;

    @Test
    void whenMonitoringObjectIsRead_thenVerifyItIsConsumed() {
        final MonitoringObjectMessage monitoringObjectMessage = mock(MonitoringObjectMessage.class);
        objectUnderTest.readMessage(monitoringObjectMessage);

        verify(monitoringObjectKafkaService, times(1)).consume(monitoringObjectMessage);
    }
}