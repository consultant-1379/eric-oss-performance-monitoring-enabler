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
package com.ericsson.oss.apps.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ericsson.oss.apps.model.MonitoringObjectMessage;
import com.ericsson.oss.apps.service.MonitoringObjectKafkaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "kafkaStartup.tls.enabled", havingValue = "false")
@Component
public class MonitoringObjectKafkaConsumer {
    private final MonitoringObjectKafkaService monitoringObjectKafkaService;

    @KafkaListener(topics = "${spring.kafka.topics.monitoringObjectTopic}", containerFactory = "kafkaListenerContainerFactory")
    public void readMessage(final MonitoringObjectMessage message) {
        log.debug("Consumed MonitoringObjectMessage from kafka topic: '{}'", message);
        monitoringObjectKafkaService.consume(message);
    }
}
