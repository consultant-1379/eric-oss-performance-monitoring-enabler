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
package com.ericsson.oss.apps.kafka.secure.utils;

import static org.apache.kafka.common.config.SslConfigs.DEFAULT_SSL_TRUSTSTORE_TYPE;
import static org.apache.kafka.common.security.auth.SecurityProtocol.SASL_SSL;
import static org.assertj.core.api.Assertions.assertThat;

import io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler;
import io.strimzi.kafka.oauth.common.Config;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class KafkaUtilsTest {

    private static final String TRUST_STORE_PATH = "c/tmp/";
    private static final String TRUST_STORE_PASSWORD = UUID.randomUUID().toString();
    private static final String TOKEN_URI = "https://localhost/auth/realms/master/protocol/openid-connect/token";
    private static final String CLIENT_ID = "rappid--53--2b4b78bc-fa3a-40d3-873b-6f7ba426f3b9";
    private static final String CLIENT_SECRET = "CA1aT1gPkB3gkiEnQqvmGLVZav7xhSUt";
    private static final String HTTPS = "https";
    private static final String PROPERTY_FORMAT = "%s=%s";

    @Test
    void whenApplySecurityProperties_thenSecurityPropertiesAdded() {
        final Map<String, Object> properties = new HashMap<>();
        assertThat(properties).isEmpty();

        KafkaUtils.applySecurityProperties(properties, TRUST_STORE_PATH, TRUST_STORE_PASSWORD);

        assertThat(properties).hasSize(5);
        assertThat(properties).containsAnyOf(
                Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SASL_SSL.name()),
                Map.entry(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, DEFAULT_SSL_TRUSTSTORE_TYPE),
                Map.entry(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, TRUST_STORE_PATH),
                Map.entry(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, TRUST_STORE_PASSWORD),
                Map.entry(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, HTTPS));
    }

    @Test
    void whenApplyOauthProperties_thenOauthPropertiesAdded() {
        final Map<String, Object> properties = new HashMap<>();
        assertThat(properties).isEmpty();

        // applyOauthProperties uses the security properties so calling it first
        KafkaUtils.applySecurityProperties(properties, TRUST_STORE_PATH, TRUST_STORE_PASSWORD);
        KafkaUtils.applyOauthProperties(properties, TOKEN_URI, CLIENT_ID, CLIENT_SECRET);

        assertThat(properties).hasSize(11);
        assertThat(properties).containsAnyOf(
                Map.entry("bearer.auth.client.id", CLIENT_ID),
                Map.entry("bearer.auth.client.secret", CLIENT_SECRET),
                Map.entry(Config.OAUTH_SCOPE, "openid"),
                Map.entry(SaslConfigs.SASL_MECHANISM, OAuthBearerLoginModule.OAUTHBEARER_MECHANISM),
                Map.entry(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, JaasClientOauthLoginCallbackHandler.class.getName()));

        final Set<String> jaasConfig = Stream.of(properties.get(SaslConfigs.SASL_JAAS_CONFIG).toString().split(" "))
                .collect(Collectors.toSet());

        assertThat(jaasConfig).containsAnyOf(
                String.format("oauth.token.endpoint.uri=\"%s\"", TOKEN_URI),
                String.format("oauth.client.id=%s", CLIENT_ID),
                String.format("clientId=%s", CLIENT_ID),
                String.format("oauth.client.secret=%s", CLIENT_SECRET),
                String.format("clientSecret=%s", CLIENT_SECRET),
                "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule",
                "required",
                String.format(PROPERTY_FORMAT, SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, DEFAULT_SSL_TRUSTSTORE_TYPE),
                String.format("oauth.ssl.truststore.type=%s", DEFAULT_SSL_TRUSTSTORE_TYPE),
                String.format(PROPERTY_FORMAT, SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, TRUST_STORE_PATH),
                String.format("oauth.ssl.truststore.location=%s", TRUST_STORE_PATH),
                String.format(PROPERTY_FORMAT, SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, TRUST_STORE_PASSWORD),
                String.format("oauth.ssl.truststore.password=%s", TRUST_STORE_PASSWORD),
                String.format(PROPERTY_FORMAT, SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, HTTPS),
                String.format("oauth.ssl.endpoint.identification.algorithm=%s", HTTPS)
        );
    }
}