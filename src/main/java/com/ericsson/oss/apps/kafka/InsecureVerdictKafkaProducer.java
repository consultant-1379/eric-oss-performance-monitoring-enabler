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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ericsson.oss.apps.model.VerdictMessage;

import lombok.extern.slf4j.Slf4j;

@ConditionalOnProperty(value = "kafkaStartup.tls.enabled", havingValue = "false")
@Slf4j
@Component
public class InsecureVerdictKafkaProducer implements VerdictKafkaProducer {
    private final String verdictTopic;
    private final KafkaTemplate<String, VerdictMessage> kafkaTemplate;

    public InsecureVerdictKafkaProducer(@Value("${spring.kafka.topics.verdictTopic}") final String verdictTopic,
                                        @Qualifier("verdictKafkaTemplate") final KafkaTemplate<String, VerdictMessage> kafkaTemplate) {
        this.verdictTopic = verdictTopic;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void sendVerdict(final VerdictMessage verdictMessage) {
        log.debug("Sending to topic {} message {}", verdictTopic, verdictMessage);
        kafkaTemplate.send(verdictTopic, verdictMessage);
    }
}
