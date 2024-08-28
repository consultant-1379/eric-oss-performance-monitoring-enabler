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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule;

import io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler;
import io.strimzi.kafka.oauth.common.Config;

import lombok.experimental.UtilityClass;

@UtilityClass
public class KafkaUtils {
    private static final String WHITE_SPACE = " ";
    private static final String[] SSL_VARIABLES = new String[] {
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG,
    };

    private static final String SASL_SSL_TEMPLATE = Arrays.stream(SSL_VARIABLES).map(variable -> variable + "=\"%s\"")
            .collect(Collectors.joining(WHITE_SPACE));
    private static final String SASL_OAUTH_SSL_TEMPLATE = Arrays.stream(SSL_VARIABLES).map(variable -> "oauth." + variable + "=\"%s\"")
            .collect(Collectors.joining(WHITE_SPACE));
    private static final String OAUTH_CREDENTIALS_TEMPLATE = "oauth.client.id=%s oauth.client.secret=\"%s\"";
    private static final String OAUTH_REGISTRY_CREDENTIALS_TEMPLATE = "clientId=%s clientSecret=\"%s\"";

    private static String fillOauthCredentials(final String template, final String clientId, final String clientSecret) {
        return String.format(template, clientId, clientSecret);
    }

    private static String fillSslTemplate(final String template, final Map<String, Object> props) {
        return String.format(template, props.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG),
                props.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG),
                props.get(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG),
                props.get(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG));
    }

    private static String getSaslJaasConfig(final String tokenUri, final String clientId,
                                            final String clientSecret, final Map<String, Object> props) {
        return String.join(WHITE_SPACE, List.of("org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required",
                String.format("oauth.token.endpoint.uri=\"%s\"", tokenUri),
                fillOauthCredentials(OAUTH_CREDENTIALS_TEMPLATE, clientId, clientSecret),
                fillOauthCredentials(OAUTH_REGISTRY_CREDENTIALS_TEMPLATE, clientId, clientSecret),
                // required for schema registry TLS (does not use the same variable names as the Kafka client)
                fillSslTemplate(SASL_SSL_TEMPLATE, props),
                // required for kafka client oauth TLS
                fillSslTemplate(SASL_OAUTH_SSL_TEMPLATE, props), ";"));
    }

    public static void applySecurityProperties(final Map<String, Object> props, final String trustStorePath,
                                               final String trustStorePassword) {
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SASL_SSL.name());
        props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, DEFAULT_SSL_TRUSTSTORE_TYPE);
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStorePath);
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, trustStorePassword);
        props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "https");
    }

    public static void applyOauthProperties(final Map<String, Object> props, final String tokenUri,
                                            final String clientId, final String clientSecret) {
        props.put(SaslConfigs.SASL_MECHANISM, OAuthBearerLoginModule.OAUTHBEARER_MECHANISM);
        props.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, JaasClientOauthLoginCallbackHandler.class.getName());
        props.put("bearer.auth.client.id", clientId);
        props.put("bearer.auth.client.secret", clientSecret);
        props.put(Config.OAUTH_SCOPE, "openid");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, getSaslJaasConfig(tokenUri, clientId, clientSecret, props));
    }

}
