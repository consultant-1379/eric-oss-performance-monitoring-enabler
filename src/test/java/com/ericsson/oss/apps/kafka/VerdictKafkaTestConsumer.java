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

import java.util.ArrayList;
import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ericsson.oss.apps.model.VerdictMessage;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class VerdictKafkaTestConsumer {
    @Getter
    private final List<VerdictMessage> messages = new ArrayList<>();

    @KafkaListener(topics = "${spring.kafka.topics.verdictTopic}", containerFactory = "verdictListenerContainerFactory")
    public void readMessage(final VerdictMessage message) {
        messages.add(message);
        log.info("Consumed verdict message ({}) : {} ", messages.size(), message);
    }
}
