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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.ericsson.oss.apps.model.VerdictMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.ericsson.oss.apps.util.avro.AvroDeserializer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@SpringBootTest(classes = {VerdictKafkaProducerConfig.class},
        properties = {"kafkaStartup.tls.enabled=true",
                "gateway.services.iam.url=" + SecureVerdictKafkaProducerConfigTest.IAM_URL,
                "gateway.services.iam.basePath=" + SecureVerdictKafkaProducerConfigTest.BASE_PATH,
                "gateway.services.iam.tokenEndpointPath=" + SecureVerdictKafkaProducerConfigTest.TOKEN_ENDPOINT_PATH})
class SecureVerdictKafkaProducerConfigTest {

    static final String IAM_URL = "https://eic.stsvp1eicXYZ.stsoss.sero.gic.ericsson.se";
    static final String BASE_PATH = "/auth/realms/master";
    static final String TOKEN_ENDPOINT_PATH = "/protocol/openid-connect/token";

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void secureProducerConfigs_thenVerifyConfigProperties() {
        final Map<String, Object> secureProducerConfig = applicationContext.getBean("secureProducerConfig", Map.class);

        assertThat(secureProducerConfig).containsAnyOf(
                Map.entry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, VerdictKafkaProducerConfig.DEFAULT_SERVERS),
                Map.entry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class),
                Map.entry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroDeserializer.class)
        );

        assertThat(secureProducerConfig.get(SaslConfigs.SASL_JAAS_CONFIG).toString())
                .contains(IAM_URL + BASE_PATH + TOKEN_ENDPOINT_PATH);

        applicationContext.getBean("secureVerdictKafkaTemplate");
    }

    @Test
    public void secureVerdictKafkaTemplate_thenKafkaTemplate() {
        final KafkaTemplate<String, VerdictMessage> kafkaTemplate = applicationContext.getBean("secureVerdictKafkaTemplate", KafkaTemplate.class);
        assertThat(kafkaTemplate).isNotNull();

        final ProducerFactory<String, VerdictMessage> producerFactory = kafkaTemplate.getProducerFactory();
        assertThat(producerFactory).isNotNull();
        assertThat(producerFactory.getConfigurationProperties()).isNotEmpty();
        assertThat(producerFactory.getConfigurationProperties())
                .contains(Map.entry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, VerdictKafkaProducerConfig.DEFAULT_SERVERS));
    }
}