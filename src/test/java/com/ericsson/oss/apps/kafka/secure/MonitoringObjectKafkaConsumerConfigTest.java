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

import static com.ericsson.oss.apps.kafka.secure.MonitoringObjectKafkaConsumerConfigTest.GROUP_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.ericsson.oss.apps.service.MessageBusService;
import com.ericsson.oss.apps.util.avro.AvroDeserializer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;

@SpringBootTest(classes = { MonitoringObjectKafkaConsumerConfig.class},
        properties = { "kafkaStartup.tls.enabled=true", "spring.kafka.consumer.group-id=" + GROUP_ID })
class MonitoringObjectKafkaConsumerConfigTest {

    static final String GROUP_ID = "test-group-id";

    private static final String MESSAGE_BUS_HOST = "bootstrap.stsvp1eic28.stsoss.sero.gic.ericsson.se:443";

    @MockBean
    private MessageBusService messageBusService;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void secureConsumerConfig_thenVerifyConfigProperties() {
        when(messageBusService.getMessageBusEndpoint()).thenReturn(MESSAGE_BUS_HOST);
        final Map<String, Object> secureConsumerConfig = applicationContext.getBean("secureConsumerConfig", Map.class);

        assertThat(secureConsumerConfig).containsAnyOf(
                Map.entry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, MESSAGE_BUS_HOST),
                Map.entry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class),
                Map.entry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroDeserializer.class),
                Map.entry(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID),
                Map.entry(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false)
        );
    }
}