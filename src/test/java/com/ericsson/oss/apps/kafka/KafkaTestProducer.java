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

package com.ericsson.oss.apps.kafka;

import com.ericsson.oss.apps.model.MonitoringObjectMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KafkaTestProducer {
    private final KafkaTemplate<String, MonitoringObjectMessage> kafkaTemplate;

    public void sendMessage(final String topic, final MonitoringObjectMessage moMessage) {
        kafkaTemplate.send(topic, moMessage);
        log.info("Kafka message has been sent on topic: '{}', message: {} ", topic, moMessage);
    }

}
