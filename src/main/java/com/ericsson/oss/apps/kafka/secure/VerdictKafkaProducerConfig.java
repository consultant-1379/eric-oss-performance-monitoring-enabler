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

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.ericsson.oss.apps.model.VerdictMessage;
import com.ericsson.oss.apps.util.avro.AvroSerializer;

import lombok.extern.slf4j.Slf4j;

@ConditionalOnProperty(value = "kafkaStartup.tls.enabled", havingValue = "true")
@Configuration
@Lazy
@Slf4j
public class VerdictKafkaProducerConfig {

    static final String DEFAULT_SERVERS = "";

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
    public Map<String, Object> secureProducerConfig() {
        final Map<String, Object> props = new HashMap<>();

        // The bootstrap.servers is empty for now, it will be updated by the KafkaStartupService.
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, DEFAULT_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class);

        final String tokenUri = iamUrl + iamBasePath + iamTokenEndpointPath;
        applySecurityProperties(props, trustStorePath, trustStorePassword);
        applyOauthProperties(props, tokenUri, clientId, clientSecret);
        return props;
    }

    private ProducerFactory<String, VerdictMessage> secureProducerFactory() {
        return new DefaultKafkaProducerFactory<>(secureProducerConfig());
    }

    @Bean
    public KafkaTemplate<String, VerdictMessage> secureVerdictKafkaTemplate() {
        return new KafkaTemplate<>(secureProducerFactory());
    }
}
