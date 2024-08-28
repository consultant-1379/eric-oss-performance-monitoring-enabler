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

import org.springframework.kafka.core.KafkaTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class InvalidKafkaProducer {
    private final KafkaTemplate<String, String> stringKafkaTemplate;

    public void sendInvalidMessage(final String topic) {
        final String data = """
                {"pmeSessionId": "1234", "fdn": "invalid schema fdn", "time": "2021-07-29T12:40:00Z", "state": "ENABLED"}
                """;
        stringKafkaTemplate.send(topic, data);
        log.info("Kafka message has been sent: '{}'", data);
    }
}
