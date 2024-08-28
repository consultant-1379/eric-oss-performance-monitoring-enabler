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

import static com.ericsson.oss.apps.kafka.secure.utils.KafkaUtils.applyOauthProperties;
import static com.ericsson.oss.apps.kafka.secure.utils.KafkaUtils.applySecurityProperties;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import com.ericsson.oss.apps.model.MonitoringObjectMessage;
import com.ericsson.oss.apps.service.MessageBusService;
import com.ericsson.oss.apps.util.avro.AvroDeserializer;

import lombok.extern.slf4j.Slf4j;

@Configuration
@ConditionalOnProperty(value = "kafkaStartup.tls.enabled", havingValue = "true")
@EnableKafka
@Lazy
@Slf4j
public class MonitoringObjectKafkaConsumerConfig {

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${tls.truststore.appStorePath}")
    private String trustStorePath;

    @Value("${tls.truststore.appStorePass}")
    private String trustStorePassword;

    @Value("${gateway.services.iam.clientId}")
    private String clientId;

    @Value("${gateway.services.iam.clientSecret}")
    private String clientSecret;

    @Value("${gateway.services.iam.url}")
    private String iamUrl;

    @Value("${gateway.services.iam.basePath}")
    private String iamBasePath;

    @Value("${gateway.services.iam.tokenEndpointPath}")
    private String iamTokenEndpointPath;

    @Bean
    public Map<String, Object> secureConsumerConfig(final MessageBusService messageBusService) {
        final Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, messageBusService.getMessageBusEndpoint());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);

        final String tokenUri = iamUrl + iamBasePath + iamTokenEndpointPath;

        applySecurityProperties(props, trustStorePath, trustStorePassword);
        applyOauthProperties(props, tokenUri, clientId, clientSecret);

        return props;
    }

    @Bean
    public ConsumerFactory<String, MonitoringObjectMessage> secureConsumerFactory(final MessageBusService messageBusService) {
        return new DefaultKafkaConsumerFactory<>(secureConsumerConfig(messageBusService), new StringDeserializer(),
                new AvroDeserializer<>(MonitoringObjectMessage.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MonitoringObjectMessage>
                                        secureKafkaListenerContainerFactory(final MessageBusService messageBusService) {
        final ConcurrentKafkaListenerContainerFactory<String, MonitoringObjectMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(secureConsumerFactory(messageBusService));
        return factory;
    }

}
