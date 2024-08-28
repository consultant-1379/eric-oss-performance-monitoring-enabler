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

package com.ericsson.oss.apps.config;

import static com.ericsson.oss.apps.util.Constants.HTTPS;
import static com.ericsson.oss.apps.util.Constants.IAM;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import lombok.val;

/**
 * Unit tests for {@link GatewayProperties} class.
 */
@ActiveProfiles("test")
@DirtiesContext
@SpringBootTest(properties = {
        "gateway.services.iam.url=https://iam.test.ericsson.se",
        "gateway.services.iam.basePath=/auth/realms/master" })
public class GatewayPropertiesTest {

    private static final String IAM_BASE_PATH = "https://iam.test.ericsson.se/auth/realms/master";
    private static final String BASE_PATH = "https://iam.test.ericsson.se";
    private static final String LOCALHOST = "localhost";
    private static final String PORT = "8080";
    private static final String SCHEME_HOST_PORT_FORMAT = "%s://%s:%s";
    private static final String SCHEME_HOST_FORMAT = "%s://%s";

    private String originalPort;
    private String originalScheme;

    @Autowired
    private GatewayProperties objectUnderTest;

    @BeforeEach
    void storeValues() {
        originalPort = objectUnderTest.getPort();
        originalScheme = objectUnderTest.getScheme();
    }

    @AfterEach
    void restoreValues() {
        objectUnderTest.setPort(originalPort);
        objectUnderTest.setScheme(originalScheme);
    }

    @Test
    void testGetUrl() {
        objectUnderTest.setPort(PORT);
        assertThat(objectUnderTest.getUrl()).isEqualTo(SCHEME_HOST_PORT_FORMAT, HTTPS, LOCALHOST, PORT);
        objectUnderTest.setPort(originalPort);
        assertThat(objectUnderTest.getUrl()).isEqualTo(SCHEME_HOST_FORMAT, HTTPS, LOCALHOST);
    }

    @Test
    void testGetUrlWithNullScheme() {
        objectUnderTest.setScheme(null);
        assertThat(objectUnderTest.getUrl()).isEqualTo(LOCALHOST);
    }

    @Test
    void testGetUrlWithEmptyScheme() {
        objectUnderTest.setScheme("");
        assertThat(objectUnderTest.getUrl()).isEqualTo(LOCALHOST);
    }

    @Test
    void testGetUrlWithEmptyPort() {
        objectUnderTest.setPort("");
        assertThat(objectUnderTest.getUrl()).isEqualTo(SCHEME_HOST_FORMAT, HTTPS, LOCALHOST);
    }

    @Test
    void testGetBasePath() {
        assertThat(objectUnderTest.getBasePath(IAM)).isEqualTo(IAM_BASE_PATH);
    }

    @Test
    public void testGetHeadersAsMap() {
        final Map<String, String> iamHeaders = objectUnderTest.getService(IAM).getHeadersAsMap();
        assertThat(iamHeaders).isEmpty();
    }

    @Test
    void testGetHeadersAsMapFromNullMap() {
        val service = objectUnderTest.getService(IAM);
        service.setHeaders(null);
        final Map<String, String> iamHeaders = service.getHeadersAsMap();
        assertThat(iamHeaders).isEmpty();
    }

    @Test
    void testGetHeadersHandlesJsonProcessingException() {
        val service = objectUnderTest.getService(IAM);
        service.setHeaders("kjlj");
        final Map<String, String> iamHeaders = service.getHeadersAsMap();
        assertThat(iamHeaders).isEmpty();
    }

    @Test
    public void testRemovalOfServicePath() {
        objectUnderTest.getBasePath(IAM);
        assertThat(objectUnderTest.removeServicePath(IAM)).isEqualTo(BASE_PATH);
    }

}