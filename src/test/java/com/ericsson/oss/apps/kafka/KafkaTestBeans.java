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

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import com.ericsson.oss.apps.model.MonitoringObjectMessage;
import com.ericsson.oss.apps.model.VerdictMessage;
import com.ericsson.oss.apps.util.avro.AvroDeserializer;
import com.ericsson.oss.apps.util.avro.AvroSerializer;

@Configuration
public class KafkaTestBeans {
    private static final String VERDICT_GROUP_ID = "pme-verdict-group";
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, VerdictMessage> verdictListenerContainerFactory() {
        final var consumerFactory = new DefaultKafkaConsumerFactory<>(
                Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroDeserializer.class,
                        ConsumerConfig.GROUP_ID_CONFIG, VERDICT_GROUP_ID,
                        ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false),
                new StringDeserializer(),
                new AvroDeserializer<>(VerdictMessage.class));

        final var factory = new ConcurrentKafkaListenerContainerFactory<String, VerdictMessage>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean
    public KafkaTestProducer validKafkaProducer() {
        final KafkaTemplate<String, MonitoringObjectMessage> template = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class)));

        return new KafkaTestProducer(template);
    }

    @Bean
    public InvalidKafkaProducer invalidKafkaProducer() {
        final KafkaTemplate<String, String> template = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)));

        return new InvalidKafkaProducer(template);
    }
}
